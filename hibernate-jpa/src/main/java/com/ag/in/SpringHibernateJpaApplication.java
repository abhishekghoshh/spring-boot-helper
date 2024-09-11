package com.ag.in;

import com.ag.in.entity.Passport;
import com.ag.in.entity.Student;
import com.ag.in.entity.StudentInformation;
import com.ag.in.repository.StudentEntityManager;
import com.ag.in.repository.StudentJdbcTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringHibernateJpaApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SpringHibernateJpaApplication.class, args);
    }

    @Autowired
    StudentJdbcTemplate studentJdbcTemplate;

    @Autowired
    StudentEntityManager studentEntityManager;

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
//        Passport passport = new Passport("A12345");
//        entityManager.persist(passport);
//        StudentInformation studentInformation = new StudentInformation("Abhishek", "Ghosh", "abhishek@gmail.com", passport);
//        entityManager.persist(studentInformation);

        StudentInformation studentInformation1 = entityManager.find(StudentInformation.class, 202);
        System.out.println(studentInformation1.getPassport());

        Passport passport1 = entityManager.find(Passport.class, 402);
        System.out.println(passport1.getStudent());

//        System.out.println(entityManager.find(Passport.class,passport.getId()));

    }

    private void usingEntityManager() {
        Student student = studentEntityManager.findById(11);
        System.out.println(student);
//        student.setLastName("Molla1");
//        System.out.println(studentEntityManager.update(student));
        student = studentEntityManager.insert(
                new Student("Abhishek", "Ghosh", "abhishek@test.com", 2)
        );
        studentEntityManager.delete(student);
        System.out.println(studentEntityManager.getAll());
    }

    private void usingJdbcTemplate() {
        try {
            Student student = new Student("Abhishek", "Ghosh", "abhishek@test.com", 2);
            System.out.println(studentJdbcTemplate.insert(student));
            System.out.println("id is " + student.getId());
            System.out.println(studentJdbcTemplate.deleteById(student.getId()));
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
