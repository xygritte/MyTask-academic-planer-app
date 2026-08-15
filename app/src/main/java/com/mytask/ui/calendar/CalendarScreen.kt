@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {

    val tasks by viewModel.tasks.collectAsState()

    /*
     * Bulan yang sedang ditampilkan.
     * Default = bulan sekarang.
     */
    var displayedMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(
                    Calendar.DAY_OF_MONTH,
                    1
                )
            }
        )
    }

    /*
     * Tanggal yang sedang dipilih.
     * Default = hari ini.
     */
    var selectedDate by remember {
        mutableStateOf(Date())
    }

    val currentMonth = displayedMonth.get(
        Calendar.MONTH
    )

    val currentYear = displayedMonth.get(
        Calendar.YEAR
    )

    /*
     * Nama bulan + tahun
     */
    val monthTitle = SimpleDateFormat(
        "MMMM yyyy",
        Locale("id", "ID")
    ).format(displayedMonth.time)

    /*
     * Jumlah hari dalam bulan
     */
    val daysInMonth =
        displayedMonth.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )

    /*
     * Hari pertama bulan.
     *
     * Calendar:
     * 1 = Minggu
     * 2 = Senin
     * ...
     * 7 = Sabtu
     */
    val firstDayOfWeek =
        displayedMonth.get(Calendar.DAY_OF_WEEK)

    /*
     * Tanggal hari ini
     */
    val today = Calendar.getInstance()

    /*
     * Apakah bulan yang sedang dibuka adalah bulan sekarang?
     */
    val isCurrentMonth =
        currentMonth ==
                today.get(Calendar.MONTH) &&
                currentYear ==
                today.get(Calendar.YEAR)

    /*
     * Task pada tanggal terpilih
     */
    val selectedTasks =
        tasks.filter {

            viewModel.isSameDay(
                it.deadline,
                selectedDate
            )
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Kalender",
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Kembali"
                        )
                    }
                },

                actions = {

                    /*
                     * Tombol kembali ke hari ini
                     */

                    IconButton(

                        onClick = {

                            val now =
                                Calendar.getInstance()

                            displayedMonth =
                                Calendar.getInstance().apply {

                                    set(
                                        Calendar.YEAR,
                                        now.get(
                                            Calendar.YEAR
                                        )
                                    )

                                    set(
                                        Calendar.MONTH,
                                        now.get(
                                            Calendar.MONTH
                                        )
                                    )

                                    set(
                                        Calendar.DAY_OF_MONTH,
                                        1
                                    )
                                }

                            selectedDate =
                                now.time
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Today,

                            contentDescription =
                                "Hari ini"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            /*
             * =========================================
             * HEADER BULAN
             * =========================================
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(

                    onClick = {

                        displayedMonth =
                            Calendar.getInstance().apply {

                                time =
                                    displayedMonth.time

                                add(
                                    Calendar.MONTH,
                                    -1
                                )

                                set(
                                    Calendar.DAY_OF_MONTH,
                                    1
                                )
                            }

                        /*
                         * Pilih tanggal 1
                         * pada bulan sebelumnya
                         */

                        selectedDate =
                            displayedMonth.time
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ChevronLeft,

                        contentDescription =
                            "Bulan sebelumnya"
                    )
                }

                Column(
                    modifier =
                        Modifier.weight(1f),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.CalendarMonth,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(

                        text =
                            monthTitle,

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                IconButton(

                    onClick = {

                        displayedMonth =
                            Calendar.getInstance().apply {

                                time =
                                    displayedMonth.time

                                add(
                                    Calendar.MONTH,
                                    1
                                )

                                set(
                                    Calendar.DAY_OF_MONTH,
                                    1
                                )
                            }

                        selectedDate =
                            displayedMonth.time
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ChevronRight,

                        contentDescription =
                            "Bulan berikutnya"
                    )
                }
            }

            /*
             * =========================================
             * HEADER HARI
             * =========================================
             */

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                listOf(
                    "Min",
                    "Sen",
                    "Sel",
                    "Rab",
                    "Kam",
                    "Jum",
                    "Sab"
                ).forEach { dayName ->

                    Text(

                        text =
                            dayName,

                        modifier =
                            Modifier.weight(1f),

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            /*
             * =========================================
             * KALENDER
             * =========================================
             */

            Column(

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                var day = 1

                /*
                 * Maksimal 6 baris.
                 */
                repeat(6) { week ->

                    Row(

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        repeat(7) { column ->

                            val cellIndex =
                                week * 7 + column

                            val firstPosition =
                                firstDayOfWeek - 1

                            /*
                             * Sel kosong sebelum hari pertama
                             */

                            if (
                                cellIndex <
                                firstPosition ||
                                day >
                                daysInMonth
                            ) {

                                Box(

                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .size(42.dp)
                                )

                            } else {

                                val currentDay =
                                    day

                                /*
                                 * Buat Date dari
                                 * tanggal yang sedang ditampilkan
                                 */

                                val dateForCell =
                                    Calendar
                                        .getInstance()
                                        .apply {

                                            set(
                                                Calendar.YEAR,
                                                currentYear
                                            )

                                            set(
                                                Calendar.MONTH,
                                                currentMonth
                                            )

                                            set(
                                                Calendar.DAY_OF_MONTH,
                                                currentDay
                                            )

                                            set(
                                                Calendar.HOUR_OF_DAY,
                                                12
                                            )

                                            set(
                                                Calendar.MINUTE,
                                                0
                                            )

                                            set(
                                                Calendar.SECOND,
                                                0
                                            )

                                            set(
                                                Calendar.MILLISECOND,
                                                0
                                            )
                                        }
                                        .time

                                /*
                                 * Ada task pada tanggal ini?
                                 */

                                val hasTask =
                                    tasks.any {

                                        viewModel.isSameDay(
                                            it.deadline,
                                            dateForCell
                                        )
                                    }

                                /*
                                 * Apakah tanggal dipilih?
                                 */

                                val isSelected =
                                    viewModel.isSameDay(
                                        selectedDate,
                                        dateForCell
                                    )

                                /*
                                 * Apakah hari ini?
                                 */

                                val isToday =
                                    viewModel.isSameDay(
                                        today.time,
                                        dateForCell
                                    )

                                Box(

                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .size(42.dp)
                                            .clip(
                                                CircleShape
                                            )
                                            .background(

                                                when {

                                                    isSelected ->
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary

                                                    else ->
                                                        MaterialTheme
                                                            .colorScheme
                                                            .surface
                                                }
                                            )
                                            .clickable {

                                                selectedDate =
                                                    dateForCell
                                            },

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Column(

                                        horizontalAlignment =
                                            Alignment.CenterHorizontally
                                    ) {

                                        Text(

                                            text =
                                                currentDay
                                                    .toString(),

                                            color =
                                                when {

                                                    isSelected ->
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onPrimary

                                                    else ->
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurface
                                                },

                                            fontWeight =
                                                if (
                                                    isToday
                                                ) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                        )

                                        /*
                                         * Titik deadline
                                         */

                                        if (
                                            hasTask
                                        ) {

                                            Box(

                                                modifier =
                                                    Modifier
                                                        .size(5.dp)
                                                        .clip(
                                                            CircleShape
                                                        )
                                                        .background(

                                                            if (
                                                                isSelected
                                                            ) {

                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .onPrimary

                                                            } else {

                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .primary
                                                            }
                                                        )
                                            )
                                        }
                                    }
                                }

                                day++
                            }
                        }
                    }
                }
            }

            /*
             * =========================================
             * TANGGAL DIPILIH
             * =========================================
             */

            Text(

                text =
                    SimpleDateFormat(
                        "EEEE, dd MMMM yyyy",
                        Locale(
                            "id",
                            "ID"
                        )
                    ).format(
                        selectedDate
                    ),

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            /*
             * =========================================
             * TASK PADA TANGGAL
             * =========================================
             */

            if (
                selectedTasks.isEmpty()
            ) {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Text(

                        text =
                            "Tidak ada tugas pada tanggal ini.",

                        modifier =
                            Modifier.padding(20.dp),

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }

            } else {

                selectedTasks.forEach { task ->

                    CalendarTaskCard(
                        task = task
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }
}


// =====================================================
// TASK CARD
// =====================================================

@Composable
private fun CalendarTaskCard(
    task: TaskEntity
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp)
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(

                text =
                    task.title,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            if (
                task.description.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        task.description,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(

                text =
                    when (task.priority) {

                        3 ->
                            "Prioritas Tinggi"

                        2 ->
                            "Prioritas Sedang"

                        else ->
                            "Prioritas Rendah"
                    },

                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )
        }
    }
}