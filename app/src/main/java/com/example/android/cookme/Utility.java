package com.example.android.cookme;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.example.android.cookme.data.Ingredient;
import com.example.android.cookme.data.Recipe;
import com.example.android.cookme.data.RecipeContract;
import com.firebase.client.Firebase;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by eduardovaca on 23/07/15.
 */
public class Utility {

    public static ContentValues createRelationshipValues(long recipe_id, long ingredient_id,
                                                     String units, double quantity){
        ContentValues relationValues = new ContentValues();
        relationValues.put(RecipeContract.RecipeIngredientRelationship.COL_RECIPE_KEY, recipe_id);
        relationValues.put(RecipeContract.RecipeIngredientRelationship.COL_INGREDIENT_KEY, ingredient_id);
        relationValues.put(RecipeContract.RecipeIngredientRelationship.COL_UNITS, units);
        relationValues.put(RecipeContract.RecipeIngredientRelationship.COL_QUANTITY, quantity);
        return relationValues;
    }

    public static ContentValues createRecipeValues(String name, String instructions, byte[] photo, String path){
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(RecipeContract.RecipeEntry.COL_NAME, name);
        recipeValues.put(RecipeContract.RecipeEntry.COL_INSTRUCTIONS, instructions);
        recipeValues.put(RecipeContract.RecipeEntry.COL_PHOTO, photo);
        recipeValues.put(RecipeContract.RecipeEntry.COL_PATH_PHOTO, path);
        return recipeValues;
    }

    public static ContentValues createIngredientValues(String name){
        ContentValues ingredientValues = new ContentValues();
        ingredientValues.put(RecipeContract.IngredientEntry.COL_NAME, name);
        return ingredientValues;
    }


    /*Method that checks if ingredient is already in DB, it return the id*/
    public static long getIngredientId(Context context, String ingredientName){

        String selection = RecipeContract.IngredientEntry.COL_NAME + " = ? COLLATE NOCASE ";
        String selectionArgs [] = new String[]{ingredientName};

        Cursor cursor = context.getContentResolver().query(
                RecipeContract.IngredientEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if(cursor.moveToFirst())
            return cursor.getLong(cursor.getColumnIndex("_id"));

        return -1L;
    }

    /*Method that insert the whole recipe to the DB*/

    public static void insertWholeRecipeInDb(Context context, String recipeName, String instructions, String photo_path,
                                             byte[] picture, ArrayList<Ingredient> ingredients){

        ContentValues recipeValues = createRecipeValues(recipeName, instructions, picture, photo_path);

        Uri recipeInserted = context.getContentResolver().insert(
                RecipeContract.RecipeEntry.CONTENT_URI, recipeValues);
        long recipe_id = ContentUris.parseId(recipeInserted);

        for(Ingredient ingredient : ingredients){
            //check if ingredient exists
            long ingredient_id = getIngredientId(context, ingredient.getName());
            if(ingredient_id == -1L){
                //Ingredient doesn't exists, so we add it
                ContentValues ingredientValues = createIngredientValues(ingredient.getName());
                Uri ingredient_inserted = context.getContentResolver().insert(
                        RecipeContract.IngredientEntry.CONTENT_URI,
                        ingredientValues);
                ingredient_id = ContentUris.parseId(ingredient_inserted);
            }else{
                Log.v(null, "USING EXISTING ID!!! " + ingredient_id);
            }

            ContentValues relationValues = createRelationshipValues(recipe_id, ingredient_id,
                                                                    ingredient.getUnits(), ingredient.getQuantity());

            context.getContentResolver().insert(RecipeContract.RecipeIngredientRelationship.CONTENT_URI,
                    relationValues);
        }

        Log.v(null, "RECIPE INSERTED!!!!");

    }

    public static void deleteRecipeFromDb(Context context, long recipeId){

        String selection = RecipeContract.RecipeEntry.TABLE_NAME + "." + RecipeContract.RecipeEntry._ID
                 + " = ? ";

        String [] selectionArgs = new String[]{Long.toString(recipeId)};

        context.getContentResolver().delete(RecipeContract.RecipeEntry.CONTENT_URI,
                                            selection,
                                            selectionArgs);

        selection = RecipeContract.RecipeIngredientRelationship.COL_RECIPE_KEY + " = ? ";

        context.getContentResolver().delete(RecipeContract.RecipeIngredientRelationship.CONTENT_URI,
                selection,
                selectionArgs);
    }


    //Method that converts from bitmap to byte array -> Obtained from StackOverflow
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    //Method that converts from byte array to bitmap -> Obtained from StackOverflow
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }



    /* Careful with this method, it deletes all records in local DB */
    public static void deleteAllRecordsFromDb(Context context){
            context.getContentResolver().delete(
                    RecipeContract.RecipeEntry.CONTENT_URI,
                    null,
                    null
            );
            context.getContentResolver().delete(
                    RecipeContract.IngredientEntry.CONTENT_URI,
                    null,
                    null
            );
            context.getContentResolver().delete(
                    RecipeContract.RecipeIngredientRelationship.CONTENT_URI,
                    null,
                    null
            );
    }

    /*
        Inserts new Recipe into the Remote Database ( Firebase )
     */

    public static void insertRecipeIntoRemoteServer(String recipeName, String instructions,
                                                    byte[] picture, ArrayList<Ingredient> ingredients){

        Firebase rootRef = new Firebase("https://cookme.firebaseio.com/recipes");

        Firebase childRef = rootRef.child(recipeName).child(recipeName);

        childRef.child("name").setValue(recipeName);
        childRef.child("instructions").setValue(instructions);
        Firebase ingredientRef =  childRef.child("ingredients");

        int numIng = 0;

        for( Ingredient i : ingredients){
            ingredientRef.child(Integer.toString( numIng)).child("ingredient name").setValue(i.getName());
            ingredientRef.child(Integer.toString( numIng)).child("ingredient quantity").setValue(i.getQuantity());
            ingredientRef.child(Integer.toString( numIng)).child("ingredient units").setValue(i.getUnits());
            numIng += 1;

        }
        String image = Base64.encodeToString(picture, Base64.URL_SAFE);
        Log.i("BAse64 is : ", image);
        image = image.replace("\n", "").replace("\r", "");
        childRef.child("image").setValue(image);

    }

    public static int getNumberIngredientsPerRecipe(Context context, long recipe_id){

        Cursor cursor = context.getContentResolver().query(
                RecipeContract.RecipeIngredientRelationship.buildRelationshipUriWithRecipeId(recipe_id),
                null,
                null,
                null,
                null);

        return cursor.getCount();
    }


}
