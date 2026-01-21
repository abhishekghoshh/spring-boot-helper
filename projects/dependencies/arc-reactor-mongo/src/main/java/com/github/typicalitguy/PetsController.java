package com.github.typicalitguy;

import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;

@RestController
@RequestMapping("/pets")
public class PetsController {
	@Autowired
	MongoTemplate mongoTemplate;

	public PetsController(@Autowired MongoTemplate mongoTemplate) {
		MongoCollection<Document> collection = mongoTemplate.getCollection("Test");
		ListIndexesIterable<Document> indexes = collection.listIndexes();
		for (Document document : indexes) {
			System.out.print(document.keySet());
		}
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public List<Pets> getAllPets() {
		return mongoTemplate.findAll(Pets.class, "User");
	}

}