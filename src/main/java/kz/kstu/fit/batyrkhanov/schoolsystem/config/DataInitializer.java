package kz.kstu.fit.batyrkhanov.schoolsystem.config;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        // Создание оценок
        if (gradeRepository.count() == 0) {
            System.out.println("Начинаем создание тестовых оценок...");

            List<Student> students = studentRepository.findAll();
            List<Teacher> teachers = teacherRepository.findAll();
            List<Subject> subjects = subjectRepository.findAll();

            if (!students.isEmpty() && !teachers.isEmpty() && !subjects.isEmpty()) {
                int totalGrades = 0;
                for (Student student : students) {
                    int gradesPerStudent = 5 + random.nextInt(3);

                    for (int i = 0; i < gradesPerStudent; i++) {
                        Subject subject = subjects.get(random.nextInt(subjects.size()));
                        Teacher teacher = teachers.get(random.nextInt(teachers.size()));
                        int gradeValue = 3 + random.nextInt(3);
                        LocalDate gradeDate = LocalDate.now().minusDays(random.nextInt(30));

                        Grade grade = new Grade(student, subject, teacher, gradeValue, gradeDate);
                        gradeRepository.save(grade);
                        totalGrades++;
                    }
                }
                System.out.println("Тестовые оценки успешно созданы! Всего добавлено: " + totalGrades + " оценок");
            }
        }

        // Создание расписания
        if (scheduleRepository.count() == 0) {
            System.out.println("Начинаем создание тестового расписания...");

            List<Subject> subjects = subjectRepository.findAll();
            List<Teacher> teachers = teacherRepository.findAll();
            List<Student> students = studentRepository.findAll();

            if (!subjects.isEmpty() && !teachers.isEmpty() && !students.isEmpty()) {
                // Получаем уникальные классы
                List<String> classes = students.stream()
                        .map(Student::getClassName)
                        .distinct()
                        .toList();

                int totalLessons = 0;
                for (String className : classes) {
                    // Генерируем расписание для каждого класса
                    DayOfWeek[] weekDays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

                    for (DayOfWeek day : weekDays) {
                        // 3-4 урока в день
                        int lessonsPerDay = 3 + random.nextInt(2);
                        LocalTime currentTime = LocalTime.of(8, 30);

                        for (int i = 0; i < lessonsPerDay && i < subjects.size(); i++) {
                            Subject subject = subjects.get(i % subjects.size());
                            Teacher teacher = teachers.get(random.nextInt(teachers.size()));
                            LocalTime endTime = currentTime.plusMinutes(90);
                            String room = "20" + (1 + random.nextInt(5));

                            Schedule schedule = new Schedule(subject, teacher, className, day,
                                    currentTime, endTime, room);
                            scheduleRepository.save(schedule);
                            totalLessons++;

                            currentTime = endTime.plusMinutes(15); // Перемена 15 минут
                        }
                    }
                }
                System.out.println("Тестовое расписание успешно создано! Всего добавлено: " + totalLessons + " уроков");
            }
        }
    }
}
