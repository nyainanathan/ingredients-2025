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
}
