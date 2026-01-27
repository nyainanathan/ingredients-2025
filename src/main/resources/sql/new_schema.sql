create type unit_type as enum('PCS', 'KG', 'L');

create table DishIngredients(
    id serial primary key ,
    id_dish int references dish(id),
    id_ingredient int references ingredient(id),
    quantity_required numeric(10,2 ),
    unit unit_type
);

alter table ingredient drop column  id_dish;

alter table ingredient drop column  required_quantity;

create type movement_type as enum ('IN', 'OUT');

create table StockMovement(
                                id serial primary key,
                                id_ingredient int references ingredient(id),
                                quantity numeric(10,2),
                                type movement_type,
                                unit unit_type,
                                creation_datetime timestamp default now()
  );