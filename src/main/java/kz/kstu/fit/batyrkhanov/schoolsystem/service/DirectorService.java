package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DirectorService {

    @Autowired private UserRepository userRepository;
    @Autowired private DirectorRepository directorRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private GradeRepository gradeRepository;
    @Autowired private AuditService auditService;

    @Transactional(readOnly = true)
    public Director findDirectorByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return null;
        return directorRepository.findByUserId(user.getId());
    }

    // === Управление учителями ===

    @Transactional(readOnly = true)
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAllWithUserAndSubjects();
    }

    @Transactional(readOnly = true)
    public Teacher getTeacherById(Long id) {
        return teacherRepository.findByIdWithUserAndSubjects(id).orElse(null);
    }

    // === Управление студентами ===

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByClassName(String className) {
        if (className == null || className.isBlank()) return List.of();
        return studentRepository.findByClassName(className);
    }

    @Transactional(readOnly = true)
    public List<String> getAllClassNames() {
        return studentRepository.findDistinctClassNames();
    }

    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    // === Управление предметами ===

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id).orElse(null);
    }

    @Transactional
    public Subject createSubject(String name, String description, String username) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название предмета не может быть пустым");
        }
        if (subjectRepository.findByName(name) != null) {
            throw new IllegalArgumentException("Предмет с таким названием уже существует");
        }
        Subject subject = new Subject(name, description);
        Subject saved = subjectRepository.save(subject);
        auditService.log("CREATE_SUBJECT", username != null ? username : "DIRECTOR", "Создан предмет: " + name);
        return saved;
    }

    @Transactional
    public Subject updateSubject(Long id, String name, String description, String username) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
        if (name != null && !name.isBlank()) {
            Subject existing = subjectRepository.findByName(name);
            if (existing != null && !existing.getId().equals(id)) {
                throw new IllegalArgumentException("Предмет с таким названием уже существует");
            }
            subject.setName(name);
        }
        subject.setDescription(description);
        Subject saved = subjectRepository.save(subject);
        auditService.log("UPDATE_SUBJECT", username != null ? username : "DIRECTOR", "Обновлён предмет ID=" + id);
        return saved;
    }

    @Transactional
    public void deleteSubject(Long id, String username) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
        subjectRepository.delete(subject);
        auditService.log("DELETE_SUBJECT", username != null ? username : "DIRECTOR", "Удалён предмет ID=" + id);
    }

    // === Расписание ===

    @Transactional(readOnly = true)
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Schedule> getScheduleByClassName(String className) {
        if (className == null || className.isBlank()) return List.of();
        return scheduleRepository.findByClassNameWithDetails(className);
    }

    @Transactional(readOnly = true)
    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id).orElse(null);
    }

    @Transactional
    public Schedule createSchedule(Long subjectId, Long teacherId, String className,
                                   String dayOfWeek, String startTime, String endTime,
                                   String roomNumber, String username) {
        if (subjectId == null || teacherId == null || className == null ||
            dayOfWeek == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Не все обязательные поля заполнены");
        }

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Учитель не найден"));

        java.time.DayOfWeek dow = java.time.DayOfWeek.valueOf(dayOfWeek);
        java.time.LocalTime start = java.time.LocalTime.parse(startTime);
        java.time.LocalTime end = java.time.LocalTime.parse(endTime);

        // Проверка дубликатов для класса
        List<Schedule> classSchedules = scheduleRepository.findByClassNameAndDayOfWeek(className, dow);
        for (Schedule existing : classSchedules) {
            if (timesOverlap(start, end, existing.getStartTime(), existing.getEndTime())) {
                throw new IllegalArgumentException("У класса " + className + " уже есть занятие в это время: " +
                    existing.getSubject().getName() + " (" + existing.getStartTime() + "-" + existing.getEndTime() + ")");
            }
        }

        // Проверка дубликатов для учителя
        List<Schedule> teacherSchedules = scheduleRepository.findByTeacherIdAndDayOfWeek(teacherId, dow);
        for (Schedule existing : teacherSchedules) {
            if (timesOverlap(start, end, existing.getStartTime(), existing.getEndTime())) {
                throw new IllegalArgumentException("У учителя " + teacher.getUser().getFullName() +
                    " уже есть занятие в это время в классе " + existing.getClassName() +
                    " (" + existing.getStartTime() + "-" + existing.getEndTime() + ")");
            }
        }

        Schedule schedule = new Schedule();
        schedule.setSubject(subject);
        schedule.setTeacher(teacher);
        schedule.setClassName(className);
        schedule.setDayOfWeek(dow);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setRoomNumber(roomNumber);

        Schedule saved = scheduleRepository.save(schedule);
        auditService.log("CREATE_SCHEDULE", username != null ? username : "DIRECTOR",
            "Создано занятие: " + className + " " + dayOfWeek + " " + startTime);
        return saved;
    }

    private boolean timesOverlap(java.time.LocalTime start1, java.time.LocalTime end1,
                                  java.time.LocalTime start2, java.time.LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    @Transactional
    public Schedule updateSchedule(Long id, Long subjectId, Long teacherId, String className,
                                   String dayOfWeek, String startTime, String endTime,
                                   String roomNumber, String username) {
        Schedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Занятие не найдено"));

        if (subjectId != null) {
            Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
            schedule.setSubject(subject);
        }
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Учитель не найден"));
            schedule.setTeacher(teacher);
        }
        if (className != null && !className.isBlank()) {
            schedule.setClassName(className);
        }
        if (dayOfWeek != null) {
            schedule.setDayOfWeek(java.time.DayOfWeek.valueOf(dayOfWeek));
        }
        if (startTime != null) {
            schedule.setStartTime(java.time.LocalTime.parse(startTime));
        }
        if (endTime != null) {
            schedule.setEndTime(java.time.LocalTime.parse(endTime));
        }
        schedule.setRoomNumber(roomNumber);

        // Проверка дубликатов для класса (кроме текущей записи)
        List<Schedule> classSchedules = scheduleRepository.findByClassNameAndDayOfWeek(
            schedule.getClassName(), schedule.getDayOfWeek());
        for (Schedule existing : classSchedules) {
            if (!existing.getId().equals(id) &&
                timesOverlap(schedule.getStartTime(), schedule.getEndTime(),
                            existing.getStartTime(), existing.getEndTime())) {
                throw new IllegalArgumentException("У класса " + schedule.getClassName() +
                    " уже есть занятие в это время: " + existing.getSubject().getName() +
                    " (" + existing.getStartTime() + "-" + existing.getEndTime() + ")");
            }
        }

        // Проверка дубликатов для учителя (кроме текущей записи)
        List<Schedule> teacherSchedules = scheduleRepository.findByTeacherIdAndDayOfWeek(
            schedule.getTeacher().getId(), schedule.getDayOfWeek());
        for (Schedule existing : teacherSchedules) {
            if (!existing.getId().equals(id) &&
                timesOverlap(schedule.getStartTime(), schedule.getEndTime(),
                            existing.getStartTime(), existing.getEndTime())) {
                throw new IllegalArgumentException("У учителя " + schedule.getTeacher().getUser().getFullName() +
                    " уже есть занятие в это время в классе " + existing.getClassName() +
                    " (" + existing.getStartTime() + "-" + existing.getEndTime() + ")");
            }
        }

        Schedule saved = scheduleRepository.save(schedule);
        auditService.log("UPDATE_SCHEDULE", username != null ? username : "DIRECTOR",
            "Обновлено занятие ID=" + id);
        return saved;
    }

    @Transactional
    public void deleteSchedule(Long id, String username) {
        Schedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Занятие не найдено"));
        scheduleRepository.delete(schedule);
        auditService.log("DELETE_SCHEDULE", username != null ? username : "DIRECTOR",
            "Удалено занятие ID=" + id);
    }

    // === Статистика ===

    @Transactional(readOnly = true)
    public long countTeachers() {
        return teacherRepository.count();
    }

    @Transactional(readOnly = true)
    public long countStudents() {
        return studentRepository.count();
    }

    @Transactional(readOnly = true)
    public long countSubjects() {
        return subjectRepository.count();
    }

    @Transactional(readOnly = true)
    public long countClasses() {
        List<String> classes = studentRepository.findDistinctClassNames();
        return classes != null ? classes.size() : 0;
    }
}

