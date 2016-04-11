package com.example.jbtang.agi.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.dao.users.User;
import com.example.jbtang.agi.dao.users.UserDBHelper;
import com.example.jbtang.agi.dao.users.UserDBManager;

import java.util.List;

public class LoginFragment extends Fragment {

    private Button loginBtn;
    private Button manageBtn;
    private EditText usernameEdt;
    private EditText passwordEdt;
    private TextView countText;
    private UserDBManager dmgr;
    private List<User> users;
    private FragmentTransaction fragmentTransaction;
    private UserManageFragment userManageFragment;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        usernameEdt = (EditText) view.findViewById(R.id.welcome_username_edit);
        passwordEdt = (EditText) view.findViewById(R.id.welcome_password_edit);
        countText = (TextView) view.findViewById(R.id.welcome_count_edit);
        loginBtn = (Button) view.findViewById(R.id.welcome_login_button);
        manageBtn = (Button) view.findViewById(R.id.welcome_manage_button);

        dmgr = new UserDBManager(getActivity());
        users = dmgr.listDB();
        if(users.size() == 0) {
            User adminUser = new User(UserDBHelper.ADMIN,UserDBHelper.ADMIN,null);
            User normalUser = new User(UserDBHelper.DEFAUL_NAME,UserDBHelper.DEFAUL_PASSWORD,UserDBHelper.DEFAUL_COUNT);
            users.add(adminUser);
            users.add(normalUser);
            dmgr.add(users);

            usernameEdt.setText(normalUser.name);
            passwordEdt.setText(normalUser.password);
            countText.setText(normalUser.count);
        }else{
            usernameEdt.setText(users.get(1).name);
            passwordEdt.setText(users.get(1).password);
            countText.setText(users.get(1).count);
        }


        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdt.getText().toString();
                String password = passwordEdt.getText().toString();
                for(User user : users){
                    if(username.equals(user.name) && password.equals(user.password)){
                        if(!username.equals(UserDBHelper.ADMIN) ){
                            int count = Integer.valueOf(user.count);
                            if(count == 0){
                                Toast.makeText(getActivity(),"剩余使用次数不足，无法登陆！",Toast.LENGTH_LONG);
                                return;
                            }else{
                                count--;
                                dmgr.updateCount(user.name, String.valueOf(count));
                            }
                        }
                        startActivity(new Intent(getActivity(),MainMenuActivity.class));
                        getActivity().finish();
                    }
                }
            }
        });
        manageBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String username = usernameEdt.getText().toString();
                String password = passwordEdt.getText().toString();
                if(username.equals("admin") && password.equals("admin")){
                    fragmentTransaction = getFragmentManager().beginTransaction();
                    userManageFragment = new UserManageFragment();
                    fragmentTransaction.replace(R.id.fragmentPager,userManageFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }
        });
        return view;
    }

    @Override
    public void onDestroy() {
        dmgr.closeDB();
        super.onDestroy();
    }
}
