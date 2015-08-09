package com.example.android.cookme;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.example.android.cookme.data.RecipeContract;
import com.example.android.cookme.data.RecipeProviderByJSON;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A placeholder fragment containing a simple view.
 */
public class LocalRecipeFragment extends Fragment implements  LoaderManager.LoaderCallbacks<Cursor>{


    private static final int RECIPE_LOADER = 0;
    private RecipeProviderByJSON mListRecipes;
    private RecipeAdapter mRecipeAdapter;
    private String mIngredientTyped;
    private String mIngredientsSelected;

    private ImageButton mSearchButton;
    private ListView mRecipesList;
    private EditText mIngredientInput;
    private TextView mIngredientsQuerying;
    private Button mClearQuery;
    public boolean InSearchMode = false;


    //Projection for querying
    private static final String[] RECIPE_COLUMNS = {
                RecipeContract.RecipeEntry.TABLE_NAME + "." + RecipeContract.RecipeEntry._ID,
                RecipeContract.RecipeEntry.COL_NAME,
                RecipeContract.RecipeEntry.COL_INSTRUCTIONS,
                RecipeContract.RecipeEntry.COL_PHOTO,
                RecipeContract.RecipeEntry.COL_PATH_PHOTO
    };

    static final int COL_RECIPE_ID = 0;
    static final int COL_RECIPE_NAME = 1;
    static final int COL_INSTRUCTIONS = 2;
    static final int COL_PHOTO = 3;
    static final int COL_PATH_PHOTO = 4;

    public LocalRecipeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_local_recipe, container, false);

        mIngredientTyped = "";
        mIngredientsSelected = "";

        mIngredientInput = (EditText) rootView.findViewById(R.id.ingredient_input);
        mIngredientsQuerying = (TextView) rootView.findViewById(R.id.ingredients_in_query_textview);
        mRecipesList = (ListView) rootView.findViewById(R.id.recipes_list);

        // The CursorAdapter will take data from our cursor and populate the ListView.
        mRecipeAdapter = new RecipeAdapter(getActivity(), null, 0);

        mRecipesList.setAdapter(mRecipeAdapter);

        mRecipesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Cursor cursor = (Cursor) adapterView.getItemAtPosition(i);
                if (cursor != null) {
                    Intent intent = new Intent(getActivity(), DetailActivity.class).
                            setData(RecipeContract.RecipeIngredientRelationship.
                                    buildRelationshipUriWithRecipeId(cursor.getLong(COL_RECIPE_ID)));
                    startActivity(intent);
                }
            }
        });

        mIngredientInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), LocalSearchActivity.class);
                startActivity(intent);
            }
        });


        return rootView;
    }

    public void restartLoader(){
        getLoaderManager().restartLoader(RECIPE_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(RECIPE_LOADER, null,  this);
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String sortOrder = RecipeContract.RecipeEntry.COL_NAME + " ASC";

        if(mIngredientsSelected.length() == 0){
            return  new CursorLoader(getActivity(),
                    RecipeContract.RecipeEntry.CONTENT_URI,
                    RECIPE_COLUMNS,
                    null,
                    null,
                    sortOrder);
        }else{
            return new CursorLoader(getActivity(),
                    RecipeContract.IngredientEntry.buildRecipesDirUri(mIngredientsSelected),
                    RECIPE_COLUMNS,
                    null,
                    null,
                    sortOrder);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mRecipeAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecipeAdapter.swapCursor(null);
    }


}
