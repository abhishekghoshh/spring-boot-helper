package io.github.abhishekghoshh.products.repository.impl;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
@Qualifier("ProductJDBCTemplate")
public class ProductJDBCTemplate {

}