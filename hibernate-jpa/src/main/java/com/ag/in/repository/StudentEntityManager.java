package com.ag.in.repository;

import com.ag.in.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class StudentEntityManager {

    // connect to database
    @PersistenceContext
    private EntityManager entityManager;

    public Student findById(long id) {
        return entityManager.find(Student.class, id);
    }

    public Student update(Student student) {
        return insertOrUpdate(student);
    }

    public Student insert(Student student) {
        return insertOrUpdate(student);
    }

    private Student insertOrUpdate(Student student) {
        if (0 == student.getId())
            entityManager.persist(student);
        else
            entityManager.merge(student);
        return student;
    }

    public void delete(Student student) {
        Student student1 = findById(student.getId());
        entityManager.remove(student1);
    }

    public List<Student> getAll() {
        TypedQuery<Student> typedQuery = entityManager.createNamedQuery("find_all_students", Student.class);
        return typedQuery.getResultList();
    }
}
