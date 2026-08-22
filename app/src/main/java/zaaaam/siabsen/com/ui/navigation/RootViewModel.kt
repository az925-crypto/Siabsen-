package zaaaam.siabsen.com.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import zaaaam.siabsen.com.security.SessionManager
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    val session: SessionManager,
) : ViewModel()
