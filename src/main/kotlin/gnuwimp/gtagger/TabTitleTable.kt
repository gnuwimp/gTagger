/*
 * Copyright 2016 - 2021 gnuwimp@gmail.com
 * Released under the GNU General Public License v3.0
 */

package gnuwimp.gtagger

import gnuwimp.swing.LayoutPanel
import gnuwimp.swing.Swing
import gnuwimp.swing.TableHeader
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * Create a table with track titles.
 */
class TabTitleTable : LayoutPanel(size = Swing.defFont.size / 2) {
    private val colSelect      = 0
    private val colTrack       = 1
    private val colTitle       = 2
    private val table          = DataTable()
    private val filterButton   = JButton(Labels.LABEL_FILTER)
    private val selectButton   = JButton(Labels.LABEL_SELECT_ALL)
    private val unselectButton = JButton(Labels.LABEL_SELECT_NONE)

    init {
        val scroll = JScrollPane()

        scroll.viewport.view = table
        add(scroll, x = 1, y = 1, w = -1, h = -6)
        add(filterButton, x = -63, y = -5, w = 20, h = 4)
        add(selectButton, x = -42, y = -5, w = 20, h = 4)
        add(unselectButton, x = -21, y = -5, w = 20, h = 4)

        filterButton.toolTipText   = Labels.TOOL_SELECT_TITLE
        selectButton.toolTipText   = Labels.TOOL_SELECT_ALL
        unselectButton.toolTipText = Labels.TOOL_SELECT_NONE

        //----------------------------------------------------------------------
        // Search and select tracks
        filterButton.addActionListener {
            val answer = JOptionPane.showInputDialog(Main.window, Labels.MESSAGE_ASK_FILTER_TITLE, Labels.DIALOG_FILTER, JOptionPane.YES_NO_OPTION)

            if (answer.isNullOrBlank() == false) {
                Data.filterOnTitles(answer)
            }
        }

        //----------------------------------------------------------------------
        // Select all tracks
        selectButton.addActionListener {
            Data.selectAll(true)
        }

        //----------------------------------------------------------------------
        // Unselect all tracks
        unselectButton.addActionListener {
            Data.selectAll(false)
        }

        //----------------------------------------------------------------------
        // Create data model for table, show list of track titles
        table.model = object : AbstractTableModel() {
            override fun getColumnClass(column: Int): Class<Any> = when (column) {
                colSelect -> true.javaClass
                else -> "".javaClass
            }

            //------------------------------------------------------------------
            override fun getColumnCount(): Int = 3

            //------------------------------------------------------------------
            override fun getColumnName(column: Int): String = when (column) {
                colSelect -> Labels.LABEL_SELECT
                colTrack -> Labels.LABEL_TRACK
                colTitle -> Labels.LABEL_TITLE
                else -> "!"
            }

            //------------------------------------------------------------------
            override fun getRowCount(): Int = Data.tracks.size

            //------------------------------------------------------------------
            override fun getValueAt(row: Int, column: Int): Any {
                val track = Data.getTrack(row)

                if (track != null) {
                    when (column) {
                        colSelect -> return track.isSelected
                        colTrack -> return track.track
                        colTitle -> return track.title
                    }
                }

                return "!"
            }

            //------------------------------------------------------------------
            override fun isCellEditable(row: Int, column: Int): Boolean = when(column) {
                colSelect -> true
                else -> false
            }

            //------------------------------------------------------------------
            override fun setValueAt(value: Any?, row: Int, column: Int) {
                val track = Data.getTrack(row)

                if (track != null && column == colSelect) {
                    track.isSelected = value as Boolean
                    Data.sendUpdate(TrackEvent.ITEM_DIRTY)
                }
            }
        }

        table.tableHeader.toolTipText = Labels.TOOL_TABLE_HEAD
        table.setColumnWidth(colSelect, min = 50, pref = 50, max = 100)
        table.setColumnWidth(colTrack, min = 50, pref = 50, max = 100)
        table.setColumnWidth(colTitle, min = 100, pref = 500, max = 10000)
        table.setShowGrid(false)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.setColumnAlign(column = colTrack, align = SwingConstants.CENTER)

        //----------------------------------------------------------------------
        // Enable table header for receiving mouse clicks so data can be sorted
        table.tableHeader.addMouseListener(object : TableHeader() {
            override fun mouseClicked(event: MouseEvent?) {
                when (columnIndex(event)) {
                    colSelect -> Data.sortTracks(Data.Sort.SELECTED, isControlDown(event))
                    colTrack -> Data.sortTracks(Data.Sort.TRACK, isControlDown(event))
                    colTitle -> Data.sortTracks(Data.Sort.TITLE, isControlDown(event))
                }

                Data.sendUpdate(TrackEvent.LIST_UPDATED)
            }
        })

        //----------------------------------------------------------------------
        // If new row has been selected do select row in Data object
        table.selectionModel.addListSelectionListener { lse ->
            val index = (lse.source as ListSelectionModel).minSelectionIndex

            if (lse.valueIsAdjusting == false && Data.selectedRow != index) {
                Data.selectedRow = index
            }
        }

        //----------------------------------------------------------------------
        // Listener callback for data changes
        Data.addListener(object : TrackListener {
            override fun update(event: TrackEvent) {
                when (event) {
                    TrackEvent.ITEM_DIRTY -> {
                        repaint()
                    }
                    TrackEvent.ITEM_SELECTED -> {
                        table.selectRow = Data.selectedRow
                    }
                    TrackEvent.LIST_UPDATED -> {
                        table.fireModel()
                        selectButton.isEnabled   = Data.tracks.isNotEmpty()
                        unselectButton.isEnabled = Data.tracks.isNotEmpty()
                        filterButton.isEnabled   = Data.tracks.isNotEmpty()
                    }
                    TrackEvent.ITEM_IMAGE -> {
                    }
                }
            }
        })
    }
}
