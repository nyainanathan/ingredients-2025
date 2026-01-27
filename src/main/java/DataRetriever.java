import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
    Dish findDishById(Integer id) {

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select dish.id as dish_id, dish.name as dish_name, dish_type, dish.price as dish_price
                            from dish
                            where dish.id = ?;
                            """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setPrice(resultSet.getObject("dish_price") == null
                        ? null : resultSet.getDouble("dish_price"));
                dish.setIngredients(findDishIngredientByDishId(id));
                return dish;
            }
            dbConnection.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                    INSERT INTO dish (id, price, name, dish_type)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name,
                        dish_type = EXCLUDED.dish_type
                    RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                }
                if (toSave.getPrice() != null) {
                    ps.setDouble(2, toSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<DishIngredient> newIngredients = toSave.getIngredients();
            detachAndAttachIngredients(conn, dishId, newIngredients);
            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void detachAndAttachIngredients(Connection conn, Integer dishId, List<DishIngredient> ingredients) throws SQLException {
        //Detach first
        PreparedStatement detachStatement = conn.prepareStatement("""
        DELETE FROM dishingredients WHERE id_dish = ?
        """);

        detachStatement.setInt(1, dishId);

        detachStatement.execute();

        //Then attach
        String attachmentQuery = """
                INSERT INTO dishingredients (id, id_dish, id_ingredient, quantity_required, unit)
                VALUES (?, ?, ?, ?, ?::unit_type)
                """;

        try {
            PreparedStatement attachStatement = conn.prepareStatement(attachmentQuery);
            for(DishIngredient ingredient : ingredients) {
                attachStatement.clearParameters();
                attachStatement.setInt(1, getNextSerialValue(conn, "dishingredients", "id"));
                attachStatement.setInt(2, dishId);
                attachStatement.setInt(3, ingredient.getId());
                attachStatement.setDouble(4, ingredient.getQuantity());
                attachStatement.setString(5, String.valueOf(ingredient.getUnit()));
                attachStatement.addBatch();
            }
            attachStatement.executeBatch();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }
        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                        INSERT INTO ingredient (id, name, category, price)
                        VALUES (?, ?, ?::ingredient_category, ?)
                        RETURNING id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    if (ingredient.getId() != null) {
                        ps.setInt(1, ingredient.getId());
                    } else {
                        ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                    }
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getPrice());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        int generatedId = rs.getInt(1);
                        ingredient.setId(generatedId);
                        savedIngredients.add(ingredient);
                    }
                }
                conn.commit();
                return savedIngredients;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(conn);
        }
    }

    private List<DishIngredient> findDishIngredientByDishId(Integer idDish) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishIngredient> ingredients = new ArrayList<>();
        try {

            PreparedStatement ps = connection.prepareStatement(
                    """
                        SELECT i.id as ingredient_id,
                               i.name as ingredient_name,
                               i.price as ingredient_price,
                               i.category as ingredient_category,
                               di.id as dish_ingredient_id,
                               di.quantity_required as quantity_required,
                               di.unit as  ingredient_unit,
                               d.id as dish_id,
                               d.name as dish_name,
                               d.dish_type as dish_type,
                               d.price as dish_price
                        FROM ingredient i
                        JOIN dishingredients di ON i.id = di.id_ingredient
                        JOIN dish d ON d.id = di.id_dish
                        WHERE di.id_dish = ?
                        """
            );

            ps.setInt(1, idDish);

            ResultSet rs =  ps.executeQuery();

            while(rs.next()) {
                DishIngredient ingredient = new DishIngredient();
                ingredient.setId(rs.getInt("dish_ingredient_id"));
                ingredient.setDish(
                        new Dish(
                            rs.getInt("dish_id"),
                                rs.getString("dish_name"),
                                DishTypeEnum.valueOf(rs.getString("dish_type")),
                                List.of(),
                                rs.getDouble("dish_price")
                        )
                );
                ingredient.setIngredient(
                        new Ingredient(
                                rs.getInt("ingredient_id"),
                                rs.getString("ingredient_name"),
                                CategoryEnum.valueOf(rs.getString("ingredient_category")),
                                rs.getDouble("ingredient_price")
                        )
                );
                ingredient.setUnit(Unit.valueOf(rs.getString("ingredient_unit")));
                ingredient.setQuantity(rs.getDouble("quantity_required"));
                ingredients.add(ingredient);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ingredients;
    }


    private String getSerialSequenceName(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sql = "SELECT pg_get_serial_sequence(?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "Any sequence found for " + tableName + "." + columnName
            );
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        String nextValSql = "SELECT nextval(?)";

        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                sequenceName, columnName, tableName
        );

        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }
}
