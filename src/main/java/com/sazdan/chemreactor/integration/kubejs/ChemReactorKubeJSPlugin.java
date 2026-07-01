package com.sazdan.chemreactor.integration.kubejs;

import com.mojang.datafixers.util.Either;
import com.sazdan.chemreactor.ChemReactorAddon;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.fluid.InputFluid;
import dev.latvian.mods.kubejs.fluid.OutputFluid;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.FluidComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;

// Мягкая (опциональная) интеграция с KubeJS: даёт скриптам модпака возможность добавлять/менять
// рецепты Chemical Reactor через event.recipes.thermal_addon_chemreactor.chem_reactor(...), так
// же, как kubejs-thermal позволяет писать event.recipes.thermal.brewer(...) для родных машин
// Thermal (см. dev.latvian.mods.kubejs.thermal.recipe.schema.BasicRecipeSchema внутри
// kubejs-thermal.jar — тот же набор полей и тех же имён JSON-полей, разобрано при исследовании).
//
// Имена JSON-полей ("ingredients"/"result"/"energy") — те же, что уже использует наш собственный
// сериализатор ChemReactorRecipeSerializers (MachineRecipeSerializer из thermal_core, общий для
// всех машин Thermal) и наш dryrubber.json — совпадение не случайное, а следствие того, что мы и
// так следовали общему формату рецептов Thermal. Значит JSON, который сгенерирует KubeJS по этой
// схеме, читается нашим же существующим ChemReactorRecipe/ChemReactorRecipeManager без изменений.
//
// Класс ссылается на классы KubeJS (dev.latvian.mods.kubejs.*), поэтому его нельзя вызывать
// напрямую из своего кода — иначе без установленного KubeJS игра упадёт с NoClassDefFoundError.
// Загрузку делает сам KubeJS: при старте он ищет в каждом загруженном моде ресурс
// kubejs.plugins.txt (см. src/main/resources/kubejs.plugins.txt) и рефлексивно создаёт
// перечисленные там классы — но только если сам KubeJS установлен. Наш код этот файл не читает и
// класс не создаёт, поэтому без KubeJS никто его не трогает.
public class ChemReactorKubeJSPlugin extends KubeJSPlugin {

    private static final RecipeKey<Either<InputFluid, InputItem>[]> INGREDIENTS =
            FluidComponents.INPUT_OR_ITEM_ARRAY.key("ingredients").alt("ingredient");
    private static final RecipeKey<Either<OutputFluid, OutputItem>[]> RESULTS =
            FluidComponents.OUTPUT_OR_ITEM_ARRAY.key("results").alt("result");
    private static final RecipeKey<Integer> ENERGY = NumberComponent.INT.key("energy");

    private static final RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS, ENERGY);

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.namespace(ChemReactorAddon.MOD_ID).register("chem_reactor", SCHEMA);
    }
}
