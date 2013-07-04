package com.tortel.deploytrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tortel.deploytrack.data.DatabaseManager;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

/**
 * The main activity that contains the fragments that show the graphs.
 * Also handles the options menu
 */
public class MainActivity extends SherlockFragmentActivity {
	private Menu settingsMenu;
	
	private DeploymentFragmentAdapter adapter;
	private ViewPager pager;
	private PageIndicator indicator;
	
	private int currentPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		adapter = new DeploymentFragmentAdapter(this, getSupportFragmentManager());
		
		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);
		
		indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);
		indicator.setOnPageChangeListener(new PageChangeListener());
	}
	
	@Override
	public void onResume(){
		super.onResume();
		reload();
	}
	
	private void reload(){
		adapter.reload();
		pager.setAdapter(adapter);
		indicator.setViewPager(pager);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.main, menu);
		settingsMenu = menu;
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent = null;
		final int id = adapter.getId(currentPosition);
		
		switch (item.getItemId()) {
		case R.id.menu_create_new:
			intent = new Intent(this, CreateActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_edit:
			//If its the info fragment, ignore
			if(id == -1){
				return true;
			}
			intent = new Intent(this, CreateActivity.class);
			intent.putExtra("id", id);
			startActivity(intent);
			return true;
		case R.id.menu_delete:
			//If its the info fragment, ignore
			if(id == -1){
				return true;
			}
			//show a nice confirmation dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.confirm);
			builder.setTitle(R.string.delete);
			builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Delete it
					DatabaseManager.getInstance(getApplicationContext()).deleteDeployment(id);
					reload();
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// No op
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
			return true;
		case R.id.menu_about:
			AboutDialogFragment about = new AboutDialogFragment();
			about.show(getSupportFragmentManager(), "about");
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent e){
		switch(keyCode){
		case KeyEvent.KEYCODE_MENU:
			settingsMenu.performIdentifierAction(R.id.full_menu_settings, 0);
			return true; 
		}
		return super.onKeyUp(keyCode, e);
	}
	
	/**
	 * Class to listen for page changes.
	 * The page number is used for editing and deleting data
	 */
	private class PageChangeListener implements ViewPager.OnPageChangeListener{
		@Override
		public void onPageSelected(int position) {
			currentPosition = position;
		}
		
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			//Ignore
		}
		@Override
		public void onPageScrollStateChanged(int state) {
			//Ignore
		}
	}
}
