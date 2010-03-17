package com.badlogic.gdx;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GDXTester extends ListActivity 
{
	String[] items = new String[]{ "Life Cycle Test", "Simple Test", "Vertex Array Test", "Vertex Buffer Object Test", "MeshRenderer Test", 
								   "Fixed Point MeshRenderer Test", "Managed Test", "Text Test"};
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState); 
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));        
	}

	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
	
		Object o = this.getListAdapter().getItem(position);
		String keyword = o.toString();

		Intent intent = null;
		if( keyword.equals( items[0] ) )
			intent = new Intent( this, LifeCycleTest.class );
		if( keyword.equals( items[1] ) )
			intent = new Intent( this, SimpleTest.class );
		if( keyword.equals( items[2] ) )
			intent = new Intent( this, VertexArrayTest.class );
		if( keyword.equals( items[3] ) )
			intent = new Intent( this, VertexBufferObjectTest.class );
		if( keyword.equals( items[4] ) )
			intent = new Intent( this, MeshRendererTest.class );
		if( keyword.equals( items[5] ) )
			intent = new Intent( this, FixedPointMeshRendererTest.class );
		if( keyword.equals( items[6] ) )
			intent = new Intent( this, ManagedTest.class );
		if( keyword.equals( items[7] ) )
			intent = new Intent( this, TextTest.class );
			
		startActivity( intent );
	}

}