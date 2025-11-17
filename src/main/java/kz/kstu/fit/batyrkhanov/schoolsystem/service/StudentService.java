package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public Student findStudentByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        Student student = studentRepository.findById(user.getId()).orElse(null);
        if (student != null) {
            // Принудительная загрузка user
            student.getUser().getFullName();
        }
        return student;
    }

    @Transactional(readOnly = true)
    public List<Grade> getStudentGrades(Student student) {
        // Используем метод с JOIN FETCH для загрузки всех связанных данных
        return gradeRepository.findByStudentOrderByGradeDateDesc(student);
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        List<Subject> subjects = subjectRepository.findAllWithTeachers();
        // Принудительная загрузка коллекции учителей для каждого предмета
        subjects.forEach(subject -> {
            subject.getTeachers().size();
        });
        return subjects;
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAllTeachers() {
        // Используем метод с JOIN FETCH для загрузки пользователей и предметов
        return teacherRepository.findAllWithUserAndSubjects();
    }

    @Transactional(readOnly = true)
    public List<Schedule> getStudentSchedule(Student student) {
        if (student == null || student.getClassName() == null) {
            return List.of();
        }
        return scheduleRepository.findByClassNameWithDetails(student.getClassName());
    }
}
