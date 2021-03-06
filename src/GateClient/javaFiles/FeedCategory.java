package es.gate;

import java.io.Serializable;

/**
 * Joao Montenegro
 * Classe que representa uma categoria.
 * Cada cardView na lista de categorias tem esta classe associada.
 */

public class FeedCategory implements Serializable {
    private String category;

    public FeedCategory(String category){
        this.category = category;
    }

    /*----Getters and setters------*/

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
