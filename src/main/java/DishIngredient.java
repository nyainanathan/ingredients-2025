public class DishIngredient {
    private int id;
    private Dish dish;
    private Ingredient ingredient;
    private double quantity;
    private Unit unit;

    public int getId() {
        return id;
    }

    public Dish getDish() {
        return dish;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public double getQuantity() {
        return quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
}
