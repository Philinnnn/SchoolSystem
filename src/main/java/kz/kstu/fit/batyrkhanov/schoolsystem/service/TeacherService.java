package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GradeRepository gradeRepository;

    @Transactional(readOnly = true)
    public Teacher findTeacherByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return null;
        return teacherRepository.findByIdWithUserAndSubjects(user.getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Subject> getTeacherSubjects(Teacher teacher) {
        if (teacher == null) return List.of();
        teacher.getSubjects().size();
        return List.copyOf(teacher.getSubjects());
    }

    @Transactional(readOnly = true)
    public List<Schedule> getTeacherSchedule(Teacher teacher) {
        if (teacher == null) return List.of();
        List<Schedule> list = scheduleRepository.findByTeacherIdWithDetails(teacher.getId());
        list = list.stream()
                .filter(s -> teacher.getSubjects() != null && teacher.getSubjects().contains(s.getSubject()))
                .collect(Collectors.toList());
        return list.stream()
                .sorted(Comparator
                        .comparing((Schedule s) -> s.getDayOfWeek().getValue())
                        .thenComparing(Schedule::getStartTime))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getTeacherClasses(Teacher teacher) {
        List<String> classes;
        if (teacher != null) {
            classes = scheduleRepository.findDistinctClassNamesByTeacherId(teacher.getId());
        } else {
            classes = List.of();
        }
        if (classes == null || classes.isEmpty()) {
            classes = studentRepository.findDistinctClassNames();
        }
        if (classes == null || classes.isEmpty()) {
            classes = scheduleRepository.findDistinctClassNamesGlobal();
        }
        if (classes == null || classes.isEmpty()) {
            classes = Arrays.asList("10А", "10Б");
        }
        return classes;
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByClassName(String className) {
        if (className == null || className.isBlank()) return List.of();
        return studentRepository.findByClassName(className);
    }

    @Transactional(readOnly = true)
    public List<Grade> getTeacherGradesForClass(Teacher teacher, String className) {
        if (teacher == null || className == null) return List.of();
        return gradeRepository.findByTeacherIdAndClassNameWithDetails(teacher.getId(), className);
    }

    @Transactional
    public Grade addGrade(Long studentId, Long subjectId, Teacher teacher, Integer gradeValue, LocalDate date) {
        if (teacher == null) throw new IllegalArgumentException("Учитель не найден");
        if (studentId == null || subjectId == null || gradeValue == null) {
            throw new IllegalArgumentException("Не все параметры указаны");
        }
        if (gradeValue < 1 || gradeValue > 5) {
            throw new IllegalArgumentException("Оценка должна быть в диапазоне 1-5");
        }
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Subject> subjectOpt = subjectRepository.findById(subjectId);
        if (studentOpt.isEmpty() || subjectOpt.isEmpty()) {
            throw new IllegalArgumentException("Студент или предмет не найден");
        }
        Student student = studentOpt.get();
        Subject subject = subjectOpt.get();
        if (!teacher.getSubjects().contains(subject)) {
            throw new IllegalStateException("Вы не ведёте выбранный предмет");
        }
        LocalDate when = date != null ? date : LocalDate.now();
        String className = student.getClassName();
        if (className == null || className.isBlank()) {
            throw new IllegalStateException("У студента не указан класс");
        }
        DayOfWeek dow = when.getDayOfWeek();
        boolean hasLesson = scheduleRepository.existsLessonForTeacherSubjectClassOnDay(
                teacher.getId(), subject.getId(), className, dow);
        if (!hasLesson) {
            throw new IllegalStateException("В выбранный день у вас нет урока по этому предмету для класса " + className);
        }
        Grade grade = new Grade(student, subject, teacher, gradeValue, when);
        return gradeRepository.save(grade);
    }

    @Transactional
    public Grade updateGrade(Long gradeId, Teacher teacher, Integer gradeValue, LocalDate date, Long subjectId) {
        Grade g = gradeRepository.findByIdWithAll(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Оценка не найдена"));
        if (!g.getTeacher().getId().equals(teacher.getId())) {
            throw new SecurityException("Нельзя изменять чужие оценки");
        }
        if (gradeValue != null) {
            if (gradeValue < 1 || gradeValue > 5) {
                throw new IllegalArgumentException("Оценка должна быть в диапазоне 1-5");
            }
            g.setGrade(gradeValue);
        }
        if (subjectId != null && (g.getSubject() == null || !g.getSubject().getId().equals(subjectId))) {
            Subject subj = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
            if (!teacher.getSubjects().contains(subj)) {
                throw new IllegalStateException("Вы не ведёте выбранный предмет");
            }
            g.setSubject(subj);
        }
        if (date != null) {
            g.setGradeDate(date);
        }
        LocalDate when = g.getGradeDate() != null ? g.getGradeDate() : LocalDate.now();
        DayOfWeek dow = when.getDayOfWeek();
        String className = g.getStudent().getClassName();
        boolean hasLesson = scheduleRepository.existsLessonForTeacherSubjectClassOnDay(
                teacher.getId(), g.getSubject().getId(), className, dow);
        if (!hasLesson) {
            throw new IllegalStateException("В выбранный день у вас нет урока по этому предмету для класса " + className);
        }
        return gradeRepository.save(g);
    }

    @Transactional
    public void deleteGrade(Long gradeId, Teacher teacher) {
        Grade g = gradeRepository.findByIdWithAll(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Оценка не найдена"));
        if (!g.getTeacher().getId().equals(teacher.getId())) {
            throw new SecurityException("Нельзя удалять чужие оценки");
        }
        gradeRepository.deleteById(gradeId);
    }
}
