package kz.kstu.fit.batyrkhanov.schoolsystem.config;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private GradeRepository gradeRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private UserRepository userRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        // 1) Очистка некорректных данных из прошлых запусков/миграций
        repairInconsistentAssignments();

        // 2) Инициализация оценок: соблюдаем (учитель -> его предметы)
        if (gradeRepository.count() == 0) {
            List<Student> students = studentRepository.findAll();
            List<Teacher> teachers = teacherRepository.findAllWithUserAndSubjects();
            if (!students.isEmpty() && !teachers.isEmpty()) {
                for (Student student : students) {
                    int gradesPerStudent = 3 + random.nextInt(3);
                    for (int i = 0; i < gradesPerStudent; i++) {
                        Teacher teacher = teachers.get(random.nextInt(teachers.size()));
                        if (teacher.getSubjects() == null || teacher.getSubjects().isEmpty()) continue;
                        List<Subject> tSubjects = new ArrayList<>(teacher.getSubjects());
                        Subject subject = tSubjects.get(random.nextInt(tSubjects.size()));
                        int gradeValue = 3 + random.nextInt(3);
                        LocalDate gradeDate = LocalDate.now().minusDays(random.nextInt(30));
                        gradeRepository.save(new Grade(student, subject, teacher, gradeValue, gradeDate));
                    }
                }
            }
        }

        // 3) Инициализация расписания: соблюдаем (учитель -> его предметы)
        if (scheduleRepository.count() == 0) {
            List<Teacher> teachers = teacherRepository.findAllWithUserAndSubjects();
            // Классы берём из студентов; если нет студентов — используем дефолтные классы
            List<String> classes = studentRepository.findAll().stream()
                    .map(Student::getClassName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (classes.isEmpty()) {
                classes = Arrays.asList("10А", "10Б");
            }
            DayOfWeek[] weekDays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

            for (Teacher t : teachers) {
                if (t.getSubjects() == null || t.getSubjects().isEmpty()) continue;
                List<Subject> tSubjects = new ArrayList<>(t.getSubjects());
                int lessonIndex = 0;
                for (String cls : classes) {
                    // Для каждого класса дадим по 1-2 занятия этого учителя в неделю
                    int lessonsPerClass = 1 + random.nextInt(2);
                    for (int i = 0; i < lessonsPerClass; i++) {
                        Subject subj = tSubjects.get(lessonIndex % tSubjects.size());
                        DayOfWeek day = weekDays[lessonIndex % weekDays.length];
                        LocalTime start = LocalTime.of(8 + (lessonIndex % 5), 0);
                        LocalTime end = start.plusMinutes(45);
                        String room = String.format("%02d%02d", 10 + (lessonIndex % 5), 1 + (lessonIndex % 4));
                        scheduleRepository.save(new Schedule(subj, t, cls, day, start, end, room));
                        lessonIndex++;
                    }
                }
            }
        }

        // 4) Подстраховка для teacher1/teacher2: если у них нет занятий, создаём по их предметам
        ensureTeacherHasSchedule("teacher1", "10А");
        ensureTeacherHasSchedule("teacher2", "10Б");
    }

    private void repairInconsistentAssignments() {
        try {
            // Удаляем оценки, где предмет не преподаётся этим учителем
            List<Grade> allGrades = gradeRepository.findAll();
            for (Grade g : allGrades) {
                Teacher t = g.getTeacher();
                Subject s = g.getSubject();
                if (t != null && s != null && (t.getSubjects() == null || !t.getSubjects().contains(s))) {
                    gradeRepository.delete(g);
                }
            }
            // Удаляем занятия, где предмет не преподаётся этим учителем
            List<Schedule> allSchedules = scheduleRepository.findAll();
            for (Schedule sc : allSchedules) {
                Teacher t = sc.getTeacher();
                Subject s = sc.getSubject();
                if (t != null && s != null && (t.getSubjects() == null || !t.getSubjects().contains(s))) {
                    scheduleRepository.delete(sc);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void ensureTeacherHasSchedule(String username, String defaultClass) {
        User u = userRepository.findByUsername(username);
        if (u == null) return;
        Teacher t = teacherRepository.findByIdWithUserAndSubjects(u.getId()).orElse(null);
        if (t == null) return;
        if (!scheduleRepository.findByTeacherIdWithDetails(t.getId()).isEmpty()) return;
        if (t.getSubjects() == null || t.getSubjects().isEmpty()) return;
        DayOfWeek[] weekDays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        int i = 0;
        for (Subject subj : t.getSubjects()) {
            DayOfWeek day = weekDays[i % weekDays.length];
            LocalTime start = LocalTime.of(9 + (i % 3), 0);
            LocalTime end = start.plusMinutes(45);
            scheduleRepository.save(new Schedule(subj, t, defaultClass, day, start, end, "10" + (i + 1)));
            i++;
        }
    }
}
