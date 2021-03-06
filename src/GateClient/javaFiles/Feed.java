package es.gate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Joao Montenegro
 * Classes mais importantes:
 * refreshFeedPosts: atualiza as publicações do feed
 * refreshFeedCategories: atualiza a lista das categorias (sem mockup ate agora)
 * onClick: Listeners mais importantes (foto de perfil, publicaçoes em si, ....)
 */

public class Feed extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, Serializable {

    private static final String TAG = "MyActivity";
    //Adapter post cards
    public PostCardsAdapter adapterPosts;
    private Context context;
    private Feed thisFeed;
    private Intent userProfile;
    private TextView userName;
    private View feedFragmentView;
    private View categoryFragmentView;
    private transient ViewPager viewPager;
    private transient SectionsPageAdapter mSectionsPageAdapter;
    //Adapter category cards
    private CategoryCardsAdapter adapterCategory;

    private FeedHomeFragment feedHomeFragment;
    private ByCategoryFragment byCategoryFragment;

    //Layouts da CardView dos posts
    private transient SwipeRefreshLayout mSwipeRefreshLayoutPosts;
    private transient RecyclerView recyclerViewPosts;

    //Layout da CardView das categorias
    private transient SwipeRefreshLayout mSwipeRefreshLayoutCategories;
    private transient RecyclerView recyclerViewCategories;

    //Lista que contem dados dos posts (input feito à mao por agora em refreshFeedPosts)
    private List<FeedPost> feedPosts;
    //Lista que contem as categorias todas (input feito à mao por agora em refreshFeedCategories)
    private List<FeedCategory> feedCategories;
    private Handler handler = new  Handler() {

        @Override
        public void handleMessage(Message msg) {
            userName.setText(msg.getData().getString("userName"), TextView.BufferType.EDITABLE);
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userName = ((TextView) findViewById(R.id.feed_user_name));
        toolbar.setVisibility(View.GONE);

        userProfile = new Intent(this, ProfileActivity.class);

        try{
            getActionBar().hide();
        }catch (NullPointerException e){
            Log.e(TAG, "ActionBar is null");
        }

        /*---Código referente às tabs do feed (Home e by category)---*/
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setVisibility(View.GONE);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        /*Gets fragment views*/
        context = this;
        thisFeed = this;
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "feedHomeFragment is " + (feedHomeFragment==null?"null":"not null"));
                feedFragmentView = feedHomeFragment.getView();
                categoryFragmentView = byCategoryFragment.getView();

                /*--------Codigo referente aos cartoes do feed----------*/
                mSwipeRefreshLayoutPosts = feedFragmentView.findViewById(R.id.swipe_container_posts);
                Log.i(TAG, "mSwipeRefreshLayoutPosts is " + (mSwipeRefreshLayoutPosts ==null?"null":"not null"));
                if (mSwipeRefreshLayoutPosts != null){
                    mSwipeRefreshLayoutPosts.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            Log.i(TAG, "Called onRefresh");
                            mSwipeRefreshLayoutPosts.setRefreshing(true); //Refresh icon gets toggled


                            try{
                                Log.i(TAG, "Refreshing data");

                                refreshFeedPosts();
                                mSwipeRefreshLayoutPosts.setRefreshing(false); //finished
                            }catch(NullPointerException e){
                                Log.e(TAG, "Error onRefresh: " + e);
                            }
                        }
                    });
                    mSwipeRefreshLayoutPosts.setColorSchemeResources(android.R.color.holo_blue_bright,
                            android.R.color.holo_green_light,
                            android.R.color.holo_orange_light,
                            android.R.color.holo_red_light);
                }

                recyclerViewPosts = feedFragmentView.findViewById(R.id.feed_post_recyclerview); //Design xml referente aos cartoes
                Log.i(TAG, "ReyclerView recyclerViewPosts is " + (recyclerViewPosts ==null?"null":"not null"));
                if ( recyclerViewPosts != null){
                    recyclerViewPosts.setHasFixedSize(true); //RecyclerView terá sempre o mesmo tamanho, performance improvement

                    LinearLayoutManager llm = new LinearLayoutManager(context); //Manager que gere como os cartoes aparecem na view
                    recyclerViewPosts.setLayoutManager(llm);

                    refreshFeedPosts(); //Por agora objectos FeedPost são criados à mao e colocados na lista feedPost

                    adapterPosts = new PostCardsAdapter(feedPosts, thisFeed); //RecyclerView adapterPosts
                    recyclerViewPosts.setAdapter(adapterPosts);
                }

                /*-------Codigo referente aos cartoes das categorias--------*/
                mSwipeRefreshLayoutCategories = categoryFragmentView.findViewById(R.id.swipe_container_categories);
                Log.i(TAG, "mSwipeRefreshLayoutCategories is " + (mSwipeRefreshLayoutCategories ==null?"null":"not null"));
                if (mSwipeRefreshLayoutCategories != null){
                    mSwipeRefreshLayoutCategories.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            Log.i(TAG, "Called onRefresh from feed categories");
                            mSwipeRefreshLayoutCategories.setRefreshing(true); //Refresh icon gets toggled


                            try{
                                Log.i(TAG, "Refreshing data");

                                refreshFeedCategories();
                                mSwipeRefreshLayoutCategories.setRefreshing(false); //finished
                            }catch(NullPointerException e){
                                Log.e(TAG, "Error onRefresh: " + e);
                            }
                        }
                    });
                    mSwipeRefreshLayoutCategories.setColorSchemeResources(android.R.color.holo_blue_bright,
                            android.R.color.holo_green_light,
                            android.R.color.holo_orange_light,
                            android.R.color.holo_red_light);
                }

                recyclerViewCategories = categoryFragmentView.findViewById(R.id.feed_category_recyclerview); //Design xml referente aos cartoes
                Log.i(TAG, "ReyclerView recyclerViewCategories is " + (recyclerViewCategories ==null?"null":"not null"));
                if ( recyclerViewCategories != null){
                    recyclerViewCategories.setHasFixedSize(true); //RecyclerView terá sempre o mesmo tamanho, performance improvement

                    LinearLayoutManager llm = new LinearLayoutManager(context); //Manager que gere como os cartoes aparecem na view
                    recyclerViewCategories.setLayoutManager(llm);

                    refreshFeedCategories(); //Por agora objectos FeedPost são criados à mao e colocados na lista feedPost

                    adapterCategory = new CategoryCardsAdapter(feedCategories); //RecyclerView adapterPosts
                    recyclerViewCategories.setAdapter(adapterCategory);
                }

            }
        }, 300);

        new Thread(new ServerConnect()).start();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_feed_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
/*
        if (id == R.id.action_search){
            return true;
        }
        */



        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupViewPager(ViewPager viewPager){
        //SectionsPageAdapter adapterPosts = new SectionsPageAdapter(getSupportFragmentManager());

        feedHomeFragment = new FeedHomeFragment();
        mSectionsPageAdapter.addFragment(feedHomeFragment, "Home");

        byCategoryFragment = new ByCategoryFragment();
        mSectionsPageAdapter.addFragment(byCategoryFragment, "By category");

        viewPager.setAdapter(mSectionsPageAdapter);
    }

    public void refreshFeedPosts(){

        Log.i(TAG, "refreshFeedPosts was called");

        //Clear old data
        if (feedPosts != null){
            feedPosts.clear();
            Log.i(TAG, "Cleared feedPosts data");
        }
        else feedPosts = new ArrayList<>();

        //Fill with new data
        Log.i(TAG, "Filling with new data");


        feedPosts.add(new FeedPost("Joao", "Livros", "Testetestetestetetste"));
        feedPosts.add(new FeedPost("Joao2", "Livros2", "gsgsgdgddhshjshjsd"));
        feedPosts.add(new FeedPost("Joao3", "Livros3", "alklaklakldfadalskalskalkdlakd"));
        feedPosts.add(new FeedPost("Joao3", "Livros3", "alklaklakldfadalskalskalkdlakd"));
        feedPosts.add(new FeedPost("Joao3", "Livros3", "alklaklakldfadalskalskalkdlakd"));
        feedPosts.add(new FeedPost("Joao3", "Livros3", "alklaklakldfadalskalskalkdlakd"));
        feedPosts.add(new FeedPost("Joao3", "Livros3", "alklaklakldfadalskalskalkdlakd"));
        feedPosts.add(new FeedPost("Joao3", "Livros3", "alklaklakldfadalskalskalkdlakd"));

        Log.i(TAG, "New data added, example: " + (feedPosts.isEmpty()?"null":feedPosts.get(0).toString()));

        //Create adapterPosts if null
        if (adapterPosts == null){
            adapterPosts = new PostCardsAdapter(feedPosts, thisFeed);
            Log.i(TAG, "New adapterPosts created");
        }
        else{
            //Notify adapterPosts of the change
            adapterPosts = new PostCardsAdapter(feedPosts, thisFeed);
            //adapterPosts.notifyDataSetChanged(); <-- Não consigo por a funcionar com este metodo
            recyclerViewPosts.setAdapter(adapterPosts);
        }
    }

    public void refreshFeedCategories(){

        Log.i(TAG, "refreshFeedCategories was called");

        //Clear old data
        if (feedCategories != null){
            feedCategories.clear();
            Log.i(TAG, "Cleared feedCategories data");
        }
        else feedCategories = new ArrayList<>();

        //Fill with new data
        Log.i(TAG, "Filling with new data");

        feedCategories.add(new FeedCategory("Category1"));
        feedCategories.add(new FeedCategory("Category2"));
        feedCategories.add(new FeedCategory("Category3"));
        feedCategories.add(new FeedCategory("Category4"));
        feedCategories.add(new FeedCategory("Category5"));
        feedCategories.add(new FeedCategory("Category6"));
        feedCategories.add(new FeedCategory("Category7"));
        feedCategories.add(new FeedCategory("Category8"));

        Log.i(TAG, "New data added, example: " + (feedCategories.isEmpty()?"null":feedCategories.get(0).toString()));

        //Create adapterPosts if null
        if (adapterCategory == null){
            adapterCategory = new CategoryCardsAdapter(feedCategories);
            Log.i(TAG, "New adapterPosts created");
        }
        else{
            //Notify adapterPosts of the change
            adapterCategory = new CategoryCardsAdapter(feedCategories);
            //adapterPosts.notifyDataSetChanged(); <-- Não consigo por a funcionar com este metodo
            recyclerViewCategories.setAdapter(adapterCategory);
        }
    }

    public void onClick(View view){
        switch (view.getId()){
            /*Ponto de interrogação no fundo do feed*/
            case R.id.feed_question_image:
                Log.i(TAG, "Triggered feed_question_image");
                break;

            /*Imagem do utilizador no feed*/
            case R.id.feed_user_image:
                Log.i(TAG, "Triggered feed_user_image");
                startActivity(userProfile);
                break;

            /*Search no topo*/
            case R.id.feed_search:
                Log.i(TAG, "Triggered feed_search");
                break;
            case R.id.feed_search_label:
                Log.i(TAG, "Triggered feed_search_label");
                break;

            /*Nova publicação, discussao e evento*/
            case R.id.feed_new_post:
                Log.i(TAG, "Triggered feed_new_post");
                break;
            case R.id.feed_new_discussion:
                Log.i(TAG, "Triggered feed_new_discussion");
                break;
            case R.id.feed_new_event:
                Log.i(TAG, "Triggered feed_new_event");
                break;

            /*Cartões de post em si*/
            case R.id.cv_post:
                Log.i(TAG, "Triggered cv_post listener");
                break;

            /*Foto do user de um post*/
            case R.id.feed_post_card_userphoto:
                Log.i(TAG, "Triggered feed_post_card_userphoto");
                break;
        }
    }

    class ServerConnect implements Runnable{

        @Override
        public void run(){
            ServerConnection srv = ServerConnection.getInstance();
            Log.d("threadRun", "start");

            String communication = String.format("!getFeedInfo");

            communication = srv.sendMessage(communication);

            Log.d("threadRun", communication);
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("userName", communication);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }
}
