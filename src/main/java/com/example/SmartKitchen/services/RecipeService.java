package com.example.SmartKitchen.services;

import com.example.SmartKitchen.dto.IngredientAmountResponseDTO;
import com.example.SmartKitchen.dto.RecipeDTO;
import com.example.SmartKitchen.dto.RecipeResponseDTO;
import com.example.SmartKitchen.models.Recipe;
import com.example.SmartKitchen.models.User;
import com.example.SmartKitchen.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final RecipeIngredientService recipeIngredientService;
    private final TagService tagService;

    public RecipeResponseDTO getRecipeById(Principal principal, Long id) {
        Recipe recipe = recipeRepository.findById(id).orElseThrow();
        if (recipe.isHiddenForOthers() && !recipe.getUser().getId().equals(userRepository.findByUsername(principal.getName()).orElseThrow().getId()))
            return null;
        return recipeToResponseDTO(recipe);
    }

    public List<RecipeResponseDTO> getAllRecipes(Principal principal) {
        List<Recipe> recipes = recipeRepository.findByUserIdOrHiddenForOthersFalse(userRepository.findByUsername(principal.getName()).orElseThrow().getId());
        return recipes.stream().map(this::recipeToResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public RecipeResponseDTO create(Principal principal, RecipeDTO dto) {
        tagService.addTags(dto.getTags());

        Recipe recipe = recipeRepository.save(Recipe.builder()
                .user(userRepository.findByUsername(principal.getName()).orElseThrow())
                .name(dto.getName())
                .imagePath(dto.getImagePath())
                .description(dto.getDescription())
                .cookSteps(dto.getCookSteps())
                .hiddenForOthers(dto.isHiddenForOthers())
                .tags(tagRepository.findByNames(dto.getTags()))
                .build());

        recipeIngredientService.updateIngredientsListInRecipe(dto.getIngredients(), recipe.getId());

        return recipeToResponseDTO(recipe);
    }

    @Transactional
    public void deleteById(Principal principal, Long id) {
        Recipe recipe = recipeRepository.findById(id).orElseThrow();
        if (recipe.getUser().getId().equals(userRepository.findByUsername(principal.getName()).orElseThrow().getId())) {
            recipeRepository.deleteById(id);
        }
    }

    public RecipeResponseDTO updateRecipe(Principal principal, Long id, RecipeDTO dto) {
        tagService.addTags(dto.getTags());

        Recipe recipe = recipeRepository.findById(id).orElseThrow();
        if (recipe.getUser().getId().equals(userRepository.findByUsername(principal.getName()).orElseThrow().getId())) {
            if (dto.getName() != null)
                recipe.setName(dto.getName());
            if (dto.getImagePath() != null)
                recipe.setImagePath(dto.getImagePath());
            if (dto.getDescription() != null)
                recipe.setDescription(dto.getDescription());
            if (dto.getCookSteps() != null)
                recipe.setCookSteps(dto.getCookSteps());
            if (dto.getTags() != null)
                recipe.setTags(tagRepository.findByNames(dto.getTags()));

            recipeIngredientService.updateIngredientsListInRecipe(dto.getIngredients(), recipe.getId());

            return recipeToResponseDTO(recipeRepository.save(recipe));
        }
        return null;
    }

    public List<RecipeResponseDTO> addRecipeToFavorite(Principal principal, Long recipeId) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Recipe> favoriteRecipes = user.getFavoriteRecipes();
        favoriteRecipes.add(recipeRepository.findById(recipeId).orElseThrow());
        user.setFavoriteRecipes(favoriteRecipes);
        userRepository.save(user);
        return favoriteRecipes.stream().map(this::recipeToResponseDTO).collect(Collectors.toList());
    }

    public List<RecipeResponseDTO> removeRecipeFromFavorite(Principal principal, Long recipeId) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Recipe> favoriteRecipes = user.getFavoriteRecipes();
        favoriteRecipes.remove(recipeRepository.findById(recipeId).orElseThrow());
        user.setFavoriteRecipes(favoriteRecipes);
        userRepository.save(user);
        return favoriteRecipes.stream().map(this::recipeToResponseDTO).collect(Collectors.toList());
    }

    private RecipeResponseDTO recipeToResponseDTO(Recipe recipe) {
        return RecipeResponseDTO.builder()
                .id(recipe.getId())
                .user(recipe.getUser())
                .name(recipe.getName())
                .imagePath(recipe.getImagePath())
                .description(recipe.getDescription())
                .cookSteps(recipe.getCookSteps())
                .hiddenForOthers(recipe.isHiddenForOthers())
                .tags(recipe.getTags())
                .ingredients(recipe.getIngredients().stream().map(e ->
                        IngredientAmountResponseDTO.builder()
                                .ingredient(e.getIngredient())
                                .amount(e.getAmount())
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }
}
