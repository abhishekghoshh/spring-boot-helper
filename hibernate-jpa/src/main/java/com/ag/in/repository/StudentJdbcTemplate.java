package com.ag.in.repository;

import com.ag.in.entity.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentJdbcTemplate {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public List<Student> getAll() {
        return jdbcTemplate.query("select * from student",
                new BeanPropertyRowMapper<>(Student.class));
    }

    public Student findById(long id) {
        return jdbcTemplate.queryForObject("select * from student where id=?",
                new BeanPropertyRowMapper<>(Student.class)
                , id);
    }

    public int insert(Student student) {
        return jdbcTemplate.update(
                "insert into student (id, first_name, last_name, email, dept_id, is_active) values(?,?,?,?,?,?)"
                , student.getId()
                , student.getFirstName()
                , student.getLastName()
                , student.getEmail()
                , student.getDeptId()
                , student.getIsActive()
        );
    }

    public int updateNameById(long id, String name) {
        return jdbcTemplate.update("update student set first_name = ? where id = ?"
                , name, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("delete from student where id = ?"
                , id);
    }
}
