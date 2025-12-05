// Classe Category
public class Category {

    private int id;        // identifiant unique dans la BDD
    private String name;   // nom de la catégorie

    // Constructeur vide → quand on veut créer l'objet petit à petit
    public Category() {
    }

    // Constructeur avec id → quand ça vient de la BDD
    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Constructeur sans id → quand on crée une nouvelle catégorie
    public Category(String name) {
        this.name = name;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;  // id assigné par la BDD
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;  // mise à jour du nom (au cas où l'admin modifie)
    }

    @Override
    public String toString() {
        return "Category { id=" + id +
               ", name='" + name + "'" +
               " }";
    }

}

