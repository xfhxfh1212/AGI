package com.example.jbtang.agi.ui;


import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.dao.users.User;
import com.example.jbtang.agi.dao.users.UserDBHelper;
import com.example.jbtang.agi.dao.users.UserDBManager;

import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class UserManageFragment extends Fragment {

    private UserDBManager dmgr;
    private List<User> users;
    private ListView listView;
    private MyAdapter myAdapter;
    private Button saveBtn;
    private Button cancelBtn;
    private Button addBtn;
    private MyListener myListener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e("TEST","Activity: "+getActivity().toString());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        dmgr.closeDB();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        dmgr = new UserDBManager(getActivity());
        users = dmgr.listDB();
        Log.e("Test","userSize" +users.size());
        View view = inflater.inflate(R.layout.fragment_user_manage, container, false);
        listView = (ListView) view.findViewById(R.id.user_manage_listView);

        myAdapter = new MyAdapter(getActivity());
        listView.setAdapter(myAdapter);
        //((MyAdapter)listView.getAdapter()).notifyDataSetChanged();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showUpdateDialog(position);
            }
        });
        saveBtn = (Button) view.findViewById(R.id.user_manage_save_button);
        addBtn = (Button) view.findViewById(R.id.user_manage_add_button);
        cancelBtn = (Button) view.findViewById(R.id.user_manage_cancel_button);
        myListener = new MyListener();
        saveBtn.setOnClickListener(myListener);
        addBtn.setOnClickListener(myListener);
        cancelBtn.setOnClickListener(myListener);
        return view;
    }
    public class MyAdapter extends BaseAdapter{
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView == null){
                convertView = mInflater.inflate(R.layout.user_manage_update_list_item,null);
                holder = new ViewHolder();
                holder.usernameText = (TextView) convertView.findViewById(R.id.user_manage_update_list_item_username);
                holder.passwordText = (TextView) convertView.findViewById(R.id.user_manage_update_list_item_password);
                holder.countText = (TextView) convertView.findViewById(R.id.user_manage_update_list_item_count);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }
            holder.usernameText.setText(users.get(position).name);
            holder.passwordText.setText(users.get(position).password);
            holder.countText.setText(users.get(position).count);
            return convertView;
        }
    }
    private final class ViewHolder{
        public TextView usernameText;
        public TextView passwordText;
        public TextView countText;
    }
    private void showUpdateDialog(int position){
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_user_manage_update,null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("")
                .setView(view)
                .show();
        final EditText usernameEdt = (EditText) view.findViewById(R.id.user_manage_username_editText);
        final EditText passwordEdt = (EditText) view.findViewById(R.id.user_manage_password_editText);
        final EditText countEdt = (EditText) view.findViewById(R.id.user_manage_count_editText);
        Button deleteBtn = (Button) view.findViewById(R.id.user_manage_update_delete_button);
        Button updateBtn = (Button) view.findViewById(R.id.user_manage_update_add_button);
        if(position != -1) {
            usernameEdt.setText(users.get(position).name);
            passwordEdt.setText(users.get(position).password);
            countEdt.setText(users.get(position).count);
            usernameEdt.setEnabled(false);
        }
        deleteBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String name = usernameEdt.getText().toString();
                Iterator iterator = users.iterator();
                while(iterator.hasNext()){
                    User user = (User) iterator.next();
                    if(user.name.equals(name) && !name.equals(UserDBHelper.ADMIN)){
                        iterator.remove();
                        break;
                    }
                }
                ((MyAdapter)listView.getAdapter()).notifyDataSetChanged();
                dialog.dismiss();
            }
        });
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(usernameEdt.getText().toString().isEmpty()
                        || passwordEdt.getText().toString().isEmpty()
                        || countEdt.getText().toString().isEmpty()
                        || !countEdt.getText().toString().matches("[0-9]*")){
                    return;
                }
                String name = usernameEdt.getText().toString();
                String password = passwordEdt.getText().toString();
                String count = countEdt.getText().toString();
                User temUser = new User(name, password, count);
                for(int i = 0; i < users.size(); i++){
                    if(users.get(i).name.equals(name)){
                        break;
                    } else if(i == users.size() - 1) {
                        users.add(temUser);
                    }
                }
                ((MyAdapter)listView.getAdapter()).notifyDataSetChanged();
                dialog.dismiss();
            }
        });
    }
    private class MyListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.user_manage_save_button:
                    saveUserUpdate();
                    break;
                case R.id.user_manage_add_button:
                    showUpdateDialog(-1);
                    break;
                case R.id.user_manage_cancel_button:
                    cancelAndBack();
                default:break;
            }
        }
    }
    private void saveUserUpdate(){
        dmgr.clear();
        dmgr.add(users);
        getFragmentManager().popBackStack();
    }
    private void cancelAndBack(){
        getFragmentManager().popBackStack();
    }
}
