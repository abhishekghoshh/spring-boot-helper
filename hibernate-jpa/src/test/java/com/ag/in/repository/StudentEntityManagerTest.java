package com.ag.in.repository;

import com.ag.in.SpringHibernateJpaApplication;
import com.ag.in.entity.Passport;
import com.ag.in.entity.Student;
import com.ag.in.entity.StudentInformation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringHibernateJpaApplication.class)
@ActiveProfiles(value = {"mysql"})
public class StudentEntityManagerTest {

    @Autowired
    StudentEntityManager studentEntityManager;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    @DirtiesContext
    @Transactional
    public void findById() {
        Student student = new Student("test", "test", "test@test.com", 1);
        entityManager.persist(student);
        Student studentCopy = studentEntityManager.findById(student.getId());
        assertEquals(student, studentCopy);
    }

    @Test
    @DirtiesContext
    @Transactional
    public void update() {
        Student student = new Student("test", "test", "test@test.com", 1);
        entityManager.persist(student);
        student.setEmail("test@gmail.com");
        studentEntityManager.update(student);
        Assert.assertEquals("test@gmail.com", entityManager.find(Student.class, student.getId()).getEmail());
    }

    @Test
    @DirtiesContext
    @Transactional
    public void insert() {
        Student student = studentEntityManager.insert(
                new Student("test", "test", "test@test.com", 1));
        System.out.println(student);
        assertNotNull(entityManager.find(Student.class, student.getId()));
    }

    @Test
    @Transactional
    public void delete() {
        Student student = entityManager.merge(new Student("test", "test", "test@test.com", 1));
        System.out.println(student);
        studentEntityManager.delete(student);
        assertNull(entityManager.find(Student.class, student.getId()));
    }

    @Test
    public void getAll() {
        assertNotNull(studentEntityManager.getAll());
    }

    @Test
//    @DirtiesContext
    @Transactional
    public void testOneToOneRelationShip() {
        Passport passport = new Passport("A12345");
        entityManager.persist(passport);
        StudentInformation studentInformation = new StudentInformation("Abhishek", "Ghosh", "abhishek@gmail.com", passport);
        entityManager.persist(studentInformation);

        StudentInformation studentInformation1 = entityManager.find(StudentInformation.class,studentInformation.getId());
        System.out.println(studentInformation1.getPassport());

        System.out.println(entityManager.find(Passport.class,passport.getId()));
    }
}