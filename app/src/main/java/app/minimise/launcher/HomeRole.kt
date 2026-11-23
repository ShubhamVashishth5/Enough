package app.minimise.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Whether this app is the default home app, and the system dialog that makes it one. */
class HomeRole @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val roleManager: RoleManager = context.getSystemService(RoleManager::class.java)

    fun isDefaultHome(): Boolean =
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)

    fun requestIntent(): Intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
}
