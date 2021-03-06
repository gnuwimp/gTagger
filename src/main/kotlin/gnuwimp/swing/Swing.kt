/*
 * Copyright 2016 - 2021 gnuwimp@gmail.com
 * Released under the GNU General Public License v3.0
 */

package gnuwimp.swing

import gnuwimp.util.TimeFormat
import gnuwimp.util.format
import java.awt.Font
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.swing.JFrame
import javax.swing.UIManager

/**
 * Quit handler, mostly for macOS
 */
class QuitHandler(val quit: () -> Unit) : InvocationHandler {
    /**
     * Call quit, this is called when cmd+q is pressed in macOS
     */
    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any? {
        quit()
        return null
    }
}

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

/**
 * About handler, mostly for macOS.
 */
class AboutHandler(val appName: String, val aboutText: String) : InvocationHandler {
    /**
     * Show dialog, this is called from the mac menu
     */
    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any? {
        show()
        return null
    }

    /**
     * Show about dialog.
     */
    fun show(width: Int = Swing.defFont.size * 60, height: Int = Swing.defFont.size * 50) {
        val dialog = HTMLDialog(html = aboutText, title = appName, width = width, height = height)
        dialog.isVisible = true
    }

    /**
     * Show about dialog.
     */
    fun show(parent: JFrame, width: Int = Swing.defFont.size * 60, height: Int = Swing.defFont.size * 50) {
        val dialog = HTMLDialog(html = aboutText, title = appName, parent = parent, width = width, height = height)
        dialog.isVisible = true
    }
}

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

/**
 * Initiate GUI stuff.
 */
object Swing {
    private var log: MutableList<String> = mutableListOf()
    private var startTime: Long = System.currentTimeMillis()
    var         defFont: Font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
    var         bigFont: Font = Font(Font.SANS_SERIF, Font.PLAIN, 28)

    /**
     * Return true if java is running on macOS.
     */
    val isMac: Boolean
        get() = !System.getProperty("os.name").toLowerCase().contains("mac")

    /**
     * Return true if java is running in an unix like operating system.
     */
    val isUnix: Boolean
        get() = !System.getProperty("os.name").toLowerCase().contains("windows")

    /**
     * Add or get log message.
     * Use empty string to clear log.
     */
    var logMessage: String
        get() {
            synchronized(log) {
                return log.joinToString("\n")
            }
        }

        set(value) {
            synchronized(log) {
                if (value.isNotEmpty() == true) {
                    log.add(TimeFormat.LONG_TIME.format(milliSeconds = System.currentTimeMillis() - startTime, timeZone = "UTC") + "| $value")
                }
                else {
                    log.clear()
                }
            }
        }

    /**
     * Run first in main method to change look and feel.
     * Setup macOS stuff.
     */
    fun setup(nativeLook: Boolean = false, appName: String = "", aboutText: String = "", quitLambda: () -> Unit = {}) {
        try {
            if (isMac == true) {
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName)
                System.setProperty("apple.awt.application.name", appName)
                System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS")
                System.setProperty("apple.laf.useScreenMenuBar", "true")

                val className    = Class.forName("com.apple.eawt.Application")
                val application  = className.getMethod("getApplication")
                val instance     = application.invoke(null)
                val aboutHandler = Proxy.newProxyInstance(Class.forName("com.apple.eawt.AboutHandler").classLoader, arrayOf(Class.forName("com.apple.eawt.AboutHandler")), AboutHandler(appName, aboutText))
                val quitHandler  = Proxy.newProxyInstance(Class.forName("com.apple.eawt.QuitHandler").classLoader, arrayOf(Class.forName("com.apple.eawt.QuitHandler")), QuitHandler(quitLambda))

                instance.javaClass.getMethod("setAboutHandler", *arrayOf(Class.forName("com.apple.eawt.AboutHandler"))).invoke(instance, *arrayOf(aboutHandler))
                instance.javaClass.getMethod("setQuitHandler", *arrayOf(Class.forName("com.apple.eawt.QuitHandler"))).invoke(instance, *arrayOf(quitHandler))
            }
        }
        catch (e: Exception) {
            println(e.message)
        }

        try {
            if (nativeLook == true) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            }
            else {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
            }
        }
        catch (e: Exception) {
            println(e.message)
        }
    }
}
