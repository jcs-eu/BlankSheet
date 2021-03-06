package com.jcs.blanksheet.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatRadioButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.jcs.blanksheet.R
import com.jcs.blanksheet.utils.Constants

/**
 * Created by Jardson Costa on 05/04/2021.
 */

class DialogSort : Dialog, RadioGroup.OnCheckedChangeListener {

    constructor(context: Context) : super(context) {}
    constructor(context: Context, @StyleRes themeResId: Int) : super(context, themeResId) {}

    private lateinit var rgSort: RadioGroup
    private lateinit var rbByNameAZ: RadioButton
    private lateinit var rbByNameZA: RadioButton
    private lateinit var rbByDateRecent: RadioButton
    private lateinit var rbByDateOld: RadioButton

    private var mPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null

    private var listener: OnOptionsChangeListener? = null


    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_sort)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      //  window!!.attributes.windowAnimations = R.style.AnimDialog
      //  window?.setGravity(Gravity.BOTTOM)

        rgSort = findViewById(R.id.rg_dialog_sort)
        rbByNameAZ = findViewById(R.id.rb_dialog_by_name_az)
        rbByNameZA = findViewById(R.id.rb_dialog_by_name_za)
        rbByDateRecent = findViewById(R.id.rb_dialog_by_date_recent)
        rbByDateOld = findViewById(R.id.rb_dialog_by_date_oldest)

        rgSort.setOnCheckedChangeListener(this)

        mPreferences = context.getSharedPreferences(Constants.RADIO_BUTTON, Context.MODE_PRIVATE)
        mEditor = this.mPreferences?.edit()

        when (mPreferences?.getString(Constants.SORT_BY, Constants.SORT_BY_NAME_AZ)) {
            Constants.SORT_BY_NAME_AZ -> {
                rbByNameAZ.isChecked = true
            }
            Constants.SORT_BY_NAME_ZA -> {
                rbByNameZA.isChecked = true
            }
            Constants.SORT_BY_DATE_RECENT -> {
                rbByDateRecent.isChecked = true
            }
            Constants.SORT_BY_DATE_OLDEST -> {
                rbByDateOld.isChecked = true
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.rb_dialog_by_name_az -> {
                mEditor?.putString(Constants.SORT_BY, Constants.SORT_BY_NAME_AZ)
                mEditor?.commit()
            }
            R.id.rb_dialog_by_name_za -> {
                mEditor?.putString(Constants.SORT_BY, Constants.SORT_BY_NAME_ZA)
                mEditor?.commit()
            }
            R.id.rb_dialog_by_date_recent -> {
                mEditor?.putString(Constants.SORT_BY, Constants.SORT_BY_DATE_RECENT)
                mEditor?.commit()
            }
            R.id.rb_dialog_by_date_oldest -> {
                mEditor?.putString(Constants.SORT_BY, Constants.SORT_BY_DATE_OLDEST)
                mEditor?.commit()
            }
        }
        listener?.onOptionsChanged()
    }

    fun setOnOptionsChangeListener(listener: OnOptionsChangeListener) {
        this.listener = listener
    }

    fun setOnOptionsChangeListener(listener: () -> Unit) {
        setOnOptionsChangeListener(object : OnOptionsChangeListener {
            override fun onOptionsChanged() {
                listener.invoke()
            }
        })
    }

    interface OnOptionsChangeListener {
        fun onOptionsChanged()
    }

}