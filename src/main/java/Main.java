import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Log before changes
        DataRetriever dataRetriever = new DataRetriever();
        Dish dish = dataRetriever.findDishById(4
        );
        System.out.println(dish);

        // Log after changes
//        dish.setIngredients(List.of(new Ingredient(1), new Ingredient(2)));
//        Dish newDish = dataRetriever.saveDish(dish);
//        System.out.println(newDish);

        // Ingredient creations
        //List<Ingredient> createdIngredients = dataRetriever.createIngredients(List.of(new Ingredient(null, "Fromage", CategoryEnum.DAIRY, 1200.0)));
        //System.out.println(createdIngredients);

        List<Dish> dishesToTest  = new ArrayList<>();
        dishesToTest.add(dataRetriever.findDishById(1));
        dishesToTest.add(dataRetriever.findDishById(2));
        dishesToTest.add(dataRetriever.findDishById(3));
        dishesToTest.add(dataRetriever.findDishById(4));
        dishesToTest.add(dataRetriever.findDishById(5));
        for(Dish d : dishesToTest) {
            System.out.println(d.getName() + " | cost: " + d.getDishCost());
            try {
            System.out.println(d.getName() + " | margin: " + d.getGrossMargin());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
