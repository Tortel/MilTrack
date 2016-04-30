/*
 * Copyright (C) 2013-2015 Scott Warner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tortel.deploytrack.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.tortel.deploytrack.Log;
import com.tortel.deploytrack.Prefs;
import com.tortel.deploytrack.R;
import com.tortel.deploytrack.data.DatabaseManager;
import com.tortel.deploytrack.data.Deployment;

/**
 * Fragment that displays the fancy deployment graph and info
 */
public class DeploymentFragment extends Fragment {
	private static final boolean DEBUG = false;
	
	private Deployment mDeployment;
	private TextView mPercentView;
	private TextView mCompletedView;
	private TextView mDateRangeView;
	private TextView mRemainingView;
	private TextView mCommaView;
	private Resources mResources;
	private DecoView mArcView;
	private int mCompletedIndex;

    private int mAnimatorType;

	/**
	 * Creates a new DeploymentFragment with the provided
	 * Deployment
	 * @param deployment
	 * @return
	 */
	public static DeploymentFragment newInstance(Deployment deployment){
		DeploymentFragment fragment = new DeploymentFragment();
		fragment.mDeployment = deployment;
		if(DEBUG){
			fragment.printAllInfo();
		}
		return fragment;
	}
	
	private void printAllInfo(){
		Log.v("Deployment id " + mDeployment.getId());
		Log.v("Start date: "+ mDeployment.getFormattedStart());
		Log.v("End date: " + mDeployment.getFormattedEnd());
		Log.v("Days complete: "+ mDeployment.getCompleted());
		Log.v("Days left: " + mDeployment.getRemaining());
		Log.v("Percentage: "+ mDeployment.getPercentage());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		mResources = getActivity().getResources();
		
		View view = inflater.inflate(R.layout.fragment_deployment, container, false);

        // Set up the views
        setUpTextViews(view);

		mDateRangeView.setText(mResources.getString(R.string.date_range,
                mDeployment.getFormattedStart(), mDeployment.getFormattedEnd()));
		mCompletedView.setText(mResources.getQuantityString(R.plurals.days_complete, mDeployment.getCompleted(), mDeployment.getCompleted()));
        mRemainingView.setText(mResources.getQuantityString(R.plurals.days_remaining, mDeployment.getRemaining(), mDeployment.getRemaining()));
		
		// Hide or display percent
		if(Prefs.hidePercent()){
		    mPercentView.setVisibility(View.GONE);
		} else {
		    mPercentView.setText(mDeployment.getPercentage() + "%");
		}
		
		//Fill the graph
		mArcView = (DecoView) view.findViewById(R.id.graph);

		SeriesItem.Builder backgroundBuilder = new SeriesItem.Builder(mDeployment.getRemainingColor())
			.setRange(0, mDeployment.getLength(), mDeployment.getLength())
			.setLineWidth(100f)
			.setInitialVisibility(false);

		mArcView.addSeries(backgroundBuilder.build());

		SeriesItem completedSeries  = new SeriesItem.Builder(mDeployment.getCompletedColor())
				.setRange(0, mDeployment.getLength(), 0)
				.setLineWidth(125f)
				.setInitialVisibility(false)
				.build();

		mCompletedIndex = mArcView.addSeries(completedSeries);

		
		return view;
	}
	
	@SuppressLint("CutPasteId")
	private void setUpTextViews(View view){
		float density = getResources().getDisplayMetrics().density;
		mCommaView = (TextView) view.findViewById(R.id.comma);
		mDateRangeView = (TextView) view.findViewById(R.id.daterange);

        TextView main = (TextView) view.findViewById(R.id.main);
        TextView second = (TextView) view.findViewById(R.id.second);
        TextView third = (TextView) view.findViewById(R.id.third);
		
		switch(Prefs.getMainDisplayType()){
		case Prefs.ViewTypes.PERCENT:
            mAnimatorType = Prefs.ViewTypes.PERCENT;
			mPercentView = main;
			mCompletedView = second;
			mRemainingView = third;
			break;
		case Prefs.ViewTypes.COMPLETE:
            mAnimatorType = Prefs.ViewTypes.COMPLETE;
			mCompletedView = main;
			mCompletedView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCompletedView.getTextSize() - density * 20f);
			mPercentView = second;
			mRemainingView = third;
			break;
		case Prefs.ViewTypes.REMAINING:
            mAnimatorType = Prefs.ViewTypes.REMAINING;
			mRemainingView = main;
			mRemainingView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mRemainingView.getTextSize() - density * 10f);
			mPercentView = second;
			mCompletedView = third;
			break;
		}

        if(Prefs.hideDate()){
            mDateRangeView.setVisibility(View.GONE);
        }

        // Just hide days
		if(Prefs.hideDays() && !Prefs.hidePercent()){
            mPercentView = main;
            mAnimatorType = Prefs.ViewTypes.PERCENT;

            // Make sure nothing else is set to main
            mCompletedView = second;
            mRemainingView = second;
            second.setVisibility(View.GONE);
            third.setVisibility(View.GONE);
            mCommaView.setVisibility(View.GONE);
            return;
        }

        // Just hide percent
        if(Prefs.hidePercent() && !Prefs.hideDays()){
            mPercentView.setVisibility(View.GONE);
            if(Prefs.getMainDisplayType() != Prefs.ViewTypes.PERCENT){
                // Hide the comma too
                mCommaView.setVisibility(View.GONE);
            }
            return;
        }

        // If both are hidden, hide it all
        if(Prefs.hideDays() && Prefs.hidePercent()){
            mCommaView.setVisibility(View.GONE);
            main.setVisibility(View.GONE);
            second.setVisibility(View.GONE);
            third.setVisibility(View.GONE);
            return;
        }
	}
	
	private ObjectAnimator getFragmentAnimator(){
		switch(mAnimatorType){
		case Prefs.ViewTypes.REMAINING:
			return ObjectAnimator.ofInt(this, "remaining", mDeployment.getLength(), mDeployment.getRemaining());
		case Prefs.ViewTypes.COMPLETE:
			return ObjectAnimator.ofInt(this, "completed", 0, mDeployment.getCompleted());
		}
		//Default is to return percent
		return ObjectAnimator.ofInt(this, "percent", 0, mDeployment.getPercentage());
	}
	
	@Override
	public void onResume(){
		super.onResume();
		animate();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(mArcView != null){
			// TODO - Re-animate the view
		}
	}
	
	public void animate(){
		if(mArcView == null || !Prefs.isAnimationEnabled()){
			setPercent(mDeployment.getPercentage());
			return;
		}
		// Remove all animation events
		mArcView.executeReset();

		mArcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true)
				.setDelay(250)
				.setDuration(1000)
				.build());

		mArcView.addEvent(new DecoEvent.Builder(mDeployment.getCompleted())
				.setIndex(mCompletedIndex)
				.setDelay(1250)
				.setDuration(1500)
				.build());

		AnimatorSet set = new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofFloat(mArcView, "percent", 0, 100),
				getFragmentAnimator()
		);
		set.setDuration(1000);
		set.start();
	}
	
	public void setCompleted(int days){
		if(mCompletedView != null && mResources != null){
			mCompletedView.setText(mResources.getQuantityString(R.plurals.days_complete, days, days));
		}
	}
	
	public void setRemaining(int days){
		if(mRemainingView != null && mResources != null){
			mRemainingView.setText(mResources.getQuantityString(R.plurals.days_remaining, days, days));
		}
	}
	
	public void setPercent(int percent){
		if(mPercentView != null){
			mPercentView.setText(percent+"%");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(savedInstanceState != null){
			int id = savedInstanceState.getInt("id");
			mDeployment = DatabaseManager.getInstance(getActivity()).getDeployment(id);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("id", mDeployment.getId());
	}
}
