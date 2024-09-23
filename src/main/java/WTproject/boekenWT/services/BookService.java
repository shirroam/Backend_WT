package WTproject.boekenWT.services;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import WTproject.boekenWT.models.*;
import WTproject.boekenWT.models.DTO.BookDTO;
import WTproject.boekenWT.models.DTO.CatalogDTO;
import WTproject.boekenWT.models.DTO.PhysicalBookCopiesDTO;
import WTproject.boekenWT.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookService {

    @Autowired
    BookRepository bookRepository;
    @Autowired
    AuthorRepository authorRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    PhysicalBookRepository physicalBookRepository;
    @Autowired
    PhysicalBookCopyRepository physicalBookCopyRepository;
    @Autowired
    AvailabilityRepository availabilityRepository;
    @Autowired
    PhysicalConditionRepository physicalConditionRepository;

    //ADD
    public String addBook(BookDTO bookTemplate) {

        Book book = new Book();
        Year year = bookTemplate.getYear();

        Set<Author> newAuthors = bookTemplate.getAuthor();
        Set<Category> newCategories = bookTemplate.getCategories();
        Book newBook = bookTemplate.getBook();


        Set<Author> authors = addAuthorsForBook(newAuthors);
        Set<Category> categories = addCategoriesForBook(newCategories);

        String add;


        // Check if the book already exists
        if(bookRepository.existsById(newBook.getIsbn())) {
            return "Book already exists";
        }
        else {
            try {
                book.setIsbn(newBook.getIsbn());
                book.setTitle(newBook.getTitle());
                for(Author newAuthor: authors) {
                    book.addAuthor(newAuthor);
                }
                for(Category newCategory : categories) {
                    book.addCategory(newCategory);
                }
                book.setYear(year);
                book.setIsOnline(newBook.getIsOnline());
                book.setIsPhysical(newBook.getIsPhysical());
                book.setSummary(newBook.getSummary());

                bookRepository.save(book);
                add = addPhysicalBook(bookTemplate, book, bookTemplate.getAmount());
            } catch (Exception e) {
                return "ErrorBS: " + e;
            }
        }

        return "Book added, "+ add;
    }

    public String addPhysicalBook(BookDTO bookTemplate, Book book, int amount) {

        PhysicalBook newPhysicalBook = new PhysicalBook();

            try {
                if(physicalBookRepository.existsPhysicalBookByIsbn(book.getIsbn()) == 1) {
                    newPhysicalBook = physicalBookRepository.findPhysicalBookByIsbn(book.getIsbn());
                    newPhysicalBook.setStock(newPhysicalBook.getStock() + amount);
                    return "Physical book already exists";
                } else {
                    newPhysicalBook.setBook(book);
                    newPhysicalBook.setArchived(false);
                    newPhysicalBook.setStock(amount);
                }

                physicalBookRepository.save(newPhysicalBook);


                PhysicalBookCopiesDTO copiesTemplate = new PhysicalBookCopiesDTO();
                copiesTemplate.setPhysicalBook(newPhysicalBook);
                copiesTemplate.setAvailability(bookTemplate.getAvailability());
                copiesTemplate.setPhysicalCondition(bookTemplate.getPhysicalCondition());
                copiesTemplate.setPurchaseDate(bookTemplate.getPurchaseDate());

                String addedCopies = addPhysicalBookCopies(copiesTemplate, amount);
                System.out.println(addedCopies);

            } catch (Exception e) {
                return "ErrorBS: " + e;
            }

        return "Physical book created!";
    }

    // ADD
    public String addPhysicalBookCopies(PhysicalBookCopiesDTO copyTemplate, int amount) {
        for(int i = 0; i < amount; i++) {
            String addedCopy = addPhysicalBookCopy(copyTemplate, i);
            System.out.println(addedCopy);
        }

        return "The given copies are created!";
    }

    // ADD
    public String addPhysicalBookCopy(PhysicalBookCopiesDTO copyTemplate, int index) {
        PhysicalBookCopy copy = new PhysicalBookCopy();

        try {
            copy.setPhysicalBook(copyTemplate.getPhysicalBook());
            // Check whether the given availability is valid
            if(availabilityRepository.existsById(copyTemplate.getAvailability().get(index).getAvailabilityId())) {
                copy.setAvailability(copyTemplate.getAvailability().get(index));
            }
            else {
                System.out.println("The given availability is invalid, so a default value is used");
                copy.setAvailability(availabilityRepository.findById(1).get());
            }
            // Check whether the given physical condition is valid
            if(physicalConditionRepository.existsById(copyTemplate.getPhysicalCondition().get(index).getPhysicalConditionId())) {
                copy.setPhysicalCondition(copyTemplate.getPhysicalCondition().get(index));
            }
            else {
                System.out.println("The given physical condition is invalid, so a default value is used");
                copy.setPhysicalCondition(physicalConditionRepository.findById(1).get());
            }
            copy.setPurchaseDate(copyTemplate.getPurchaseDate().get(index));

            physicalBookCopyRepository.save(copy);
        } catch (Exception e) {
            return "ErrorBS: " + e;
        }

        return "A copy is created!";
    }

    //GET
    public List<PhysicalBookCopy> getBookInfo(int isbn) {
        List<PhysicalBookCopy> physicalBookCopies = new ArrayList<>();
        if(bookRepository.existsById(isbn)) {
            return physicalBookCopyRepository.findCopiesByIsbn(isbn);
        }
        else {
            return physicalBookCopies;
        }
    }

    public List<Book> getAllBooks() {
        return (List<Book>)bookRepository.findAll();
    }

    //UPDATE
    public String updateBookData(BookDTO bookTemplate) {
        Set<Author> authors = new HashSet<Author>();
        Set<Category> categories = new HashSet<Category>();

        int isbn = bookTemplate.getBook().getIsbn();
        Year newYear = bookTemplate.getYear();
        Set<Author> newAuthors = bookTemplate.getAuthor();
        Set<Category> newCategories = bookTemplate.getCategories();
        Book newBook = bookTemplate.getBook();

        if(bookRepository.existsById(isbn)) {

            // Check if the author already exists
            for(Author newAuthor: newAuthors) {
                Author author = new Author();
                if (authorRepository.existsById(newAuthor.getAuthorId())) {
                    // Fetch the existing author from the database
                    author = authorRepository.findById(newAuthor.getAuthorId()).get();
                    authors.add(author);
                } else {
                    // Create and save the new author
                    author.setName(newAuthor.getName());
                    try {
                        author = authorRepository.save(author);
                        authors.add(author);

                        // Save and get the managed entity
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }
            }
            // Check if the category already exists
            for(Category newCategory: newCategories) {
                Category category = new Category();
                if (categoryRepository.existsById(newCategory.getCategoryId())) {
                    // Fetch the existing category from the database
                    category = categoryRepository.findById(newCategory.getCategoryId()).get();
                    categories.add(category);
                } else {
                    // Create and save the new category
                    category.setCategory(newCategory.getCategory());
                    try {
                        category = categoryRepository.save(category);
                        categories.add(category);

                        // Save and get the managed entity
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }
            }

            Book updatedBook = bookRepository.findById(isbn).get();
            try {
                updatedBook.setIsbn(newBook.getIsbn());
                updatedBook.setTitle(newBook.getTitle());
                for(Author newAuthor: authors) {
                    updatedBook.addAuthor(newAuthor);
                }
                for(Category newCategory : categories) {
                    updatedBook.addCategory(newCategory);
                }
                updatedBook.setYear(newYear);
                updatedBook.setIsOnline(newBook.getIsOnline());
                updatedBook.setIsPhysical(newBook.getIsPhysical());
                updatedBook.setSummary(newBook.getSummary());

                bookRepository.save(updatedBook);
            } catch (Exception e) {
                return "ErrorBS: " + e;
            }
        } else {
            return "This book does not yet exist, update failed.";
        }

        return "book id " + bookTemplate.getBook()+ " updated.";
    }

    //DELETE
    public String deleteBookItem(int isbn) {
        if (bookRepository.existsById(isbn)) {
            try {
                bookRepository.deleteById(isbn);
                //Nog doen: ook de authors van dit boek moeten verwijderd worden uit the authorrepository als ze niet gekoppeld zijn aan een ander boek.
                return "Book deleted";
            } catch (Exception e) {
                return "Error: " + e;
            }
        }
        else {
            return "Book not found";
        }
    }

    public Set<Author> addAuthorsForBook(Set<Author> newAuthors) {
        Set<Author> authors = new HashSet<>();

        // Check if the author already exists
        for(Author newAuthor: newAuthors) {
            Author author = new Author();
            if (authorRepository.existsById(newAuthor.getAuthorId())) {
                // Fetch the existing author from the database
                author = authorRepository.findById(newAuthor.getAuthorId()).get();
                authors.add(author);
            } else {
                // Create and save the new author
                author.setName(newAuthor.getName());
                try {
                    author = authorRepository.save(author);
                    authors.add(author);

                    // Save and get the managed entity
                } catch (Exception e) {
                    System.out.println("Error: " + e);
                    return authors; // Return empty set
                }
            }
        }
        return authors;
    }

    public Set<Category> addCategoriesForBook(Set<Category> newCategories) {
        Set<Category> categories = new HashSet<>();
        // Check if the category already exists
        for(Category newCategory: newCategories) {
            Category category = new Category();
            if (categoryRepository.existsById(newCategory.getCategoryId())) {
                // Fetch the existing category from the database
                category = categoryRepository.findById(newCategory.getCategoryId()).get();
                categories.add(category);
            } else {
                // Create and save the new category
                category.setCategory(newCategory.getCategory());
                try {
                    category = categoryRepository.save(category);
                    categories.add(category);

                    // Save and get the managed entity
                } catch (Exception e) {
                    System.out.println("Error: " + e);
                    return categories; // Return empty set
                }
            }
        }
        return categories;
    }

    public List<CatalogDTO> getAllCatalogData() {
        Iterable<Book> books=bookRepository.findAll();

        List<CatalogDTO> listCatalog=new ArrayList();
        for (Book b:books) {
            CatalogDTO cd = new CatalogDTO(b);
            listCatalog.add(cd);
        }
        return listCatalog;
    }
}
