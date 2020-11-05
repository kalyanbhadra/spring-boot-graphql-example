package com.kals.graphql.services;


import com.kals.graphql.model.Book;
import com.kals.graphql.repository.BookRepository;
import com.kals.graphql.services.datafetcher.AllBookDataFetcher;
import com.kals.graphql.services.datafetcher.BookDataFetcher;
import graphql.GraphQL;
import graphql.language.TypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

@Service
public class GraphQLService {

    @Value("classpath:books.graphql")
    Resource resource;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    private AllBookDataFetcher allBookDataFetcher;
    @Autowired
    private BookDataFetcher bookDataFetcher;

    private GraphQL graphQL;

    // load schema at application startup
    @PostConstruct
    private void loadSchema() throws IOException {
        // load books into repository
        loadDataIntoHSQL();

        // get the schema from the file we wrote (book schema)
        File schemaFile = resource.getFile();
        // parse schema
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schemaFile);
        RuntimeWiring wiring = buildRuntimeWiring();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        graphQL = GraphQL.newGraphQL(schema).build();
    }

    private void loadDataIntoHSQL() {
        Stream.of(
                new Book("123", "Book of Clouds", "Kindle Edition",
                        new String[] {
                                "Chloe Aridjis"
                        }, "12-12-2000"),
                new Book("124", "Game of Thrones", "Kathmandu",
                        new String[] {
                                "Mike Aurelius", "Devid Jones"
                        }, "12-05-1850"),
                new Book("124", "Understand Woman", "Unbelivable",
                        new String[] {
                                "I died", "Never woke up"
                        }, "12-05-1850")
        ).forEach(book -> {
            bookRepository.save(book);
        });
    }

    private RuntimeWiring buildRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> {
                    return typeWiring
                            .dataFetcher("allBooks", allBookDataFetcher)
                            .dataFetcher("book", bookDataFetcher);
                })
                .build();
    }

    public GraphQL getGraphQL() {
        return graphQL;
    }
}
