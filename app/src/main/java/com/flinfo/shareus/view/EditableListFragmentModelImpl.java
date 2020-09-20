package com.flinfo.shareus.view;

import com.flinfo.shareus.fragment.EditableListFragment;
import com.flinfo.shareus.widget.EditableListAdapter;

public interface EditableListFragmentModelImpl<V extends EditableListAdapter.EditableViewHolder>
{
    void setLayoutClickListener(EditableListFragment.LayoutClickListener<V> clickListener);
}
