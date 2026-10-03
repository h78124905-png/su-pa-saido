package com.example.fanlauncher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.*

object AppPickerDialog {
    data class AppEntry(val pkg: String, val label: String, val icon: Drawable)

    fun show(context: Context, current: List<String>, max: Int, onResult: (List<String>) -> Unit) {
        val pm = context.packageManager
        val entries = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString(), pm.getApplicationIcon(it)) }
            .sortedBy { it.label }

        val selected = current.toMutableSet()
        val list = ListView(context).apply {
            choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            adapter = object : BaseAdapter() {
                override fun getCount() = entries.size
                override fun getItem(position: Int) = entries[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val row = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(24, 16, 24, 16)
                    }
                    row.addView(CheckBox(context).apply { isChecked = selected.contains(entries[position].pkg) },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.18f))
                    row.addView(TextView(context).apply {
                        text = entries[position].label
                        textSize = 16f
                        compoundDrawablePadding = 24
                        setCompoundDrawablesWithIntrinsicBounds(entries[position].icon, null, null, null)
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.82f))
                    return row
                }
            }
        }

        val adapter = list.adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val pkg = entries[position].pkg
            if (selected.contains(pkg)) selected.remove(pkg)
            else if (selected.size < max) selected.add(pkg)
            else Toast.makeText(context, "最大" + max + "個まで", Toast.LENGTH_SHORT).show()
            (adapter as BaseAdapter).notifyDataSetChanged()
        }

        AlertDialog.Builder(context)
            .setTitle("アプリを選択（最大" + max + "個）")
            .setView(list)
            .setPositiveButton("決定") { _, _ -> onResult(selected.toList()) }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}
