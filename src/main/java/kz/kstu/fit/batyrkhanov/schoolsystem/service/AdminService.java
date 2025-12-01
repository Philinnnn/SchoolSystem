package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.SchoolClass;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.SchoolClassRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.StudentRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    // ===== Управление классами =====

    public List<SchoolClass> getAllClasses() {
        return schoolClassRepository.findAll();
    }

    public SchoolClass getClassById(Long id) {
        return schoolClassRepository.findById(id).orElse(null);
    }

    @Transactional
    public SchoolClass createClass(String name, String username) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название класса не может быть пустым");
        }

        name = name.trim();

        if (schoolClassRepository.existsByName(name)) {
            throw new IllegalArgumentException("Класс с таким названием уже существует");
        }

        SchoolClass schoolClass = new SchoolClass(name);
        SchoolClass saved = schoolClassRepository.save(schoolClass);

        auditService.log("CREATE_CLASS", username, "Создан класс: " + name);

        return saved;
    }

    @Transactional
    public SchoolClass updateClass(Long id, String newName, String username) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Класс не найден"));

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Название класса не может быть пустым");
        }

        newName = newName.trim();

        if (!schoolClass.getName().equals(newName) && schoolClassRepository.existsByName(newName)) {
            throw new IllegalArgumentException("Класс с таким названием уже существует");
        }

        String oldName = schoolClass.getName();
        schoolClass.setName(newName);

        // Обновляем className у всех студентов этого класса
        List<Student> students = studentRepository.findByClassName(oldName);
        for (Student student : students) {
            student.setClassName(newName);
            studentRepository.save(student);
        }

        SchoolClass saved = schoolClassRepository.save(schoolClass);

        auditService.log("UPDATE_CLASS", username,
            "Изменен класс: " + oldName + " → " + newName);

        return saved;
    }

    @Transactional
    public void deleteClass(Long id, String username) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Класс не найден"));

        // Проверяем, есть ли студенты в этом классе
        List<Student> students = studentRepository.findByClassName(schoolClass.getName());
        if (!students.isEmpty()) {
            throw new IllegalArgumentException(
                "Невозможно удалить класс: в нём " + students.size() + " студентов. " +
                "Сначала переместите или удалите студентов."
            );
        }

        String className = schoolClass.getName();
        schoolClassRepository.delete(schoolClass);

        auditService.log("DELETE_CLASS", username, "Удален класс: " + className);
    }

    // ===== Управление классом студента =====

    @Transactional
    public void assignStudentToClass(Long studentId, String className, String username) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        if (className != null && !className.trim().isEmpty()) {
            // Проверяем существование класса
            if (!schoolClassRepository.existsByName(className.trim())) {
                throw new IllegalArgumentException("Класс не найден: " + className);
            }
        }

        String oldClass = student.getClassName();
        student.setClassName(className != null ? className.trim() : null);
        studentRepository.save(student);

        auditService.log("ASSIGN_STUDENT_CLASS", username,
            "Студент " + student.getUser().getFullName() +
            ": " + (oldClass != null ? oldClass : "без класса") +
            " → " + (className != null ? className : "без класса"));
    }

    public List<Student> getStudentsWithoutClass() {
        return studentRepository.findByClassNameIsNull();
    }

    public List<Student> getStudentsByClassName(String className) {
        return studentRepository.findByClassName(className);
    }
}

