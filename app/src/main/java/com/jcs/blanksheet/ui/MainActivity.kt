package com.jcs.blanksheet.ui

//import org.koin.androidx.viewmodel.compat.SharedViewModelCompat.sharedViewModel

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jcs.blanksheet.DocumentApplication
import com.jcs.blanksheet.R
import com.jcs.blanksheet.adapter.BlankSheetAdapter
import com.jcs.blanksheet.callbacks.EventClick
import com.jcs.blanksheet.dialog.BottomSortDialog
import com.jcs.blanksheet.dialog.sharedpreference.SharedPreferenceLiveData.SharedPreferenceStringLiveData
import com.jcs.blanksheet.model.Document
import com.jcs.blanksheet.utils.Constants
import com.jcs.blanksheet.utils.JcsUtils
import com.jcs.blanksheet.utils.Sort
import com.jcs.blanksheet.viewmodel.DocumentViewModel
import com.jcs.blanksheet.widget.JcsSearchView
import com.jcs.blanksheet.widget.ToastUndo
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), EventClick.OnClickListener,
    EventClick.OnLongClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardView: CardView
    private lateinit var searchView: JcsSearchView

    private lateinit var adapter: BlankSheetAdapter
    //  private lateinit var dao: DocumentDao
    // private var listDoc = ArrayList<Document>()

    private var actionMode: ActionMode? = null

    private var reloadList: Boolean = false

    private val viewModel: DocumentViewModel by viewModels {
        DocumentViewModel.DocViewModelFactory((application as DocumentApplication).repository)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar_main)
        fabAdd = findViewById(R.id.main_add_fab)
        searchView = findViewById(R.id.search_view)
        recyclerView = findViewById(R.id.recycler_view)
        cardView = findViewById(R.id.card_view_main)
        setUpToolbar()

        //recyclerView.setHasFixedSize(true)
        adapter = BlankSheetAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter.setOnClickListener(this)
        adapter.setOnLongClickListener(this)

        fabAdd.setOnClickListener {
            val i = Intent(this, EditorActivity::class.java)
            startActivityForResult(i, Constants.REQUEST_CODE_RELOAD_LIST)
        }

        recyclerView.setOnScrollChangeListener { _, _, _, _, oldScrollY ->
            if (oldScrollY > 0 && actionMode == null)
                fabAdd.show()
            else if (oldScrollY < 0)
                fabAdd.hide()
        }

        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val sharedPreferenceStringLiveData = SharedPreferenceStringLiveData(
            preferences,
            Constants.SORT_BY,
            Sort.get(0)!!
        )

        sharedPreferenceStringLiveData.getString(Constants.SORT_BY, Sort.get(0)!!)
            .observe(this) { sortBy ->
                loadList(sortBy)
            }

        setSearchView()
    }

    private fun loadList(sortBy: String) {
        println(sortBy)
        viewModel.getAllDocuments(sortBy).observe(this) { docs ->
            docs.let {
                adapter.submitList(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_RELOAD_LIST) {
            if (resultCode == Constants.RESULT_CODE_RELOAD_LIST) {
                reloadList = data!!.getBooleanExtra(Constants.RESULT_RELOAD_LIST_KEY, false)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_menu_main_sort -> {
                BottomSortDialog(this).apply {
                    setOnOptionsChangeListener {
                        dismiss()
                    }
                    show()
                }
            }
            R.id.item_menu_main_search -> {
                searchView.openSearch()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (searchView.isOpen)
            searchView.closeSearch()
        else
            super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        if (reloadList) {
            //    loadDocuments()
            reloadList = false
        }
    }

    override fun onDocumentClick(position: Int, document: Document, cardView: MaterialCardView) {
        if (actionMode != null) {
            this.toggleSelection(position)
        } else {
            val intentEditor = Intent(this, EditorActivity::class.java)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                cardView,
                Constants.ANIMATION_CONTAINER_KEY
            )
            intentEditor.putExtra(Constants.DOCUMENT_ID, document.id)
            startActivity(intentEditor, options.toBundle())
        }
    }

    override fun onDocumentLongClick(position: Int, document: Document) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
            this.toggleSelection(position)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.menu_selection, menu)
            fabAdd.hide()
            adapter.isSelectionMode(true)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.item_menu_selection_delete -> {
                    deleteDocument()
                }
            }
            actionMode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            fabAdd.show()
            adapter.clearSelection()
            adapter.isSelectionMode(false)
            actionMode = null
        }
    }
//  created methods

    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position)
        val count = adapter.getSelectedItemCount()

        if (count == 0) {
            actionMode?.finish()
        } else {
            actionMode?.title = count.toString()
            actionMode?.invalidate()
        }
    }

    private fun setSearchView() {
        searchView.apply {
            adjustTintAlpha(0.8f)
            setTextColor(ContextCompat.getColor(context, R.color.appbar_color_icon))
            setOnQueryTextListener(object : JcsSearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {

                    val textSearch = newText.decapitalize(Locale.ROOT)
                    val listSearch = ArrayList<Document>()
                    for (i in adapter.currentList) {
                        if (i.title.decapitalize(Locale.ROOT).contains(textSearch)) {
                            listSearch.add(i)
                        }
                        //  adapter = BlankSheetAdapter(context, listSearch)
                        adapter.setOnClickListener { position, document, cardView ->
                            val intentEditor = Intent(context, EditorActivity::class.java)
                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this@MainActivity,
                                cardView,
                                Constants.ANIMATION_CONTAINER_KEY
                            )
                            intentEditor.putExtra(Constants.DOCUMENT_ID, document.id)
                            startActivity(intentEditor, options.toBundle())
                        }
                        recyclerView.adapter = adapter
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    clearFocus()
                    closeSearch()
                    //       loadDocuments()
                    return true
                }
            })

            setSearchViewListener(object : JcsSearchView.SearchViewListener {
                override fun onSearchViewOpened() {
                    fabAdd.hide()
                }

                override fun onSearchViewClosed() {
                    fabAdd.show()
                    //    loadDocuments()
                }
            })
        }
    }

    private fun deleteDocument() {
        val checkedDocument: List<Document> = adapter.getCheckedDocuments()
        viewModel.deleteDocument(*checkedDocument.toTypedArray())
        ToastUndo(this, fabAdd).apply {
            setOnUndoClickListener {
                for (i in checkedDocument) {
                    viewModel.saveDocument(i)
                    dismiss()
                }
            }
            show()
        }
    }

//    private fun onDeleteDocument() {
//        val checkedDocument: List<Document> = adapter.getCheckedDocuments()
//        val toastUndo = ToastUndo(this, fabAdd)
//
//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                dao.deleteDocument(*checkedDocument.toTypedArray())
//
//                toastUndo.setOnUndoClickListener {
//                    // dao.saveDocument(*checkedDocument.toTypedArray())
////                    for (doc in checkedDocument) {
////
////                       // this.launch { dao.saveDocument(doc) }
////                    }
//                }
//            }
//            runOnUiThread {
//                toastUndo.show()
//                //  loadDocuments()
//            }
//        }


//        if (checkedDocument.isNotEmpty()) {
//            dao.deleteDocument(*checkedDocument.toTypedArray())
//            loadDocuments()
//            ToastUndo(this, fabAdd).apply {
//                setOnUndoClickListener {
//                    for (document in checkedDocument) {
//                        dao.saveDocument(document)
//                        loadDocuments()
//                        it.dismiss()
//                    }
//                }
//                this.show()
//            }
//        }
    //   }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        val customTitleLayout = layoutInflater.inflate(R.layout.custom_title, null)
        val customTitle = customTitleLayout.findViewById<TextView>(R.id.custom_title_text)
        val appName = SpannableString(resources.getText(R.string.app_name))
        appName.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorSecondary)),
            0,
            5,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        appName.setSpan(StyleSpan(Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        customTitle.text = appName
        supportActionBar?.customView = customTitleLayout

    }

}