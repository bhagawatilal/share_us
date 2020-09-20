package com.flinfo.shareus.view;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.flinfo.shareus.R;
import com.flinfo.shareus.adapter.ImageListAdapter;
import com.flinfo.shareus.adapter.GroupEditableListFragment;
import com.flinfo.shareus.widget.GroupEditableListAdapter;

import java.util.Map;

abstract public class GalleryGroupEditableListFragment<T extends GroupEditableListAdapter.GroupShareable, V extends GroupEditableListAdapter.GroupViewHolder, E extends GroupEditableListAdapter<T, V>>
        extends GroupEditableListFragment<T, V, E>
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setDefaultGroupingCriteria(ImageListAdapter.MODE_GROUP_BY_DATE);
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        super.onGroupingOptions(options);

        options.put(getString(R.string.text_groupByNothing), ImageListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), ImageListAdapter.MODE_GROUP_BY_DATE);
        options.put(getString(R.string.text_groupByAlbum), ImageListAdapter.MODE_GROUP_BY_ALBUM);
    }
}
