package co.netguru.chatroulette.feature.main.video

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.view.View
import co.netguru.chatroulette.R
import co.netguru.chatroulette.app.App
import co.netguru.chatroulette.feature.base.BaseMvpFragment
import co.netguru.chatroulette.webrtc.service.WebRtcService
import kotlinx.android.synthetic.main.fragment_video.*
import timber.log.Timber


class VideoFragment : BaseMvpFragment<VideoFragmentView, VideoFragmentPresenter>(), VideoFragmentView, ServiceConnection {

    companion object {
        val TAG: String = VideoFragment::class.java.name

        fun newInstance() = VideoFragment()
    }

    override fun getLayoutId() = R.layout.fragment_video

    override fun retrievePresenter() = App.getApplicationComponent(context).videoFragmentComponent().videoFragmentPresenter()

    var service: WebRtcService? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (buttonPanel.layoutParams as CoordinatorLayout.LayoutParams).behavior = MoveUpBehavior()
        activity.volumeControlStream = AudioManager.STREAM_VOICE_CALL
        connectButton.setOnClickListener {
            getPresenter().connect()
        }
        disconnectButton.setOnClickListener {
            getPresenter().disconnectByUser()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        service?.detachViews()
        unbindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!activity.isChangingConfigurations) {
            service?.stopSelf()
        }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        service = null
        Timber.d("Service disconnected")
    }

    override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
        Timber.d("Service connected")
        service = (iBinder as (WebRtcService.LocalBinder)).service
        service?.attachLocalView(localVideoView)
        service?.attachRemoteView(remoteVideoView)
        getPresenter().startRoulette()
    }

    override fun connectTo(uuid: String) {
        service?.offerDevice(uuid)
    }

    override fun attachService() {
        val intent = Intent(activity, WebRtcService::class.java)
        context.startService(intent)
        context.applicationContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun disconnect() {
        service?.let {
            unbindService()
            it.stopSelf()
        }
    }

    private fun unbindService() {
        context.applicationContext.unbindService(this)
        service = null
    }

    override fun showCamViews() {
        buttonPanel.visibility = View.VISIBLE
        camView.visibility = View.VISIBLE
        connectButton.visibility = View.GONE
    }

    override fun showStartRouletteView() {
        buttonPanel.visibility = View.GONE
        camView.visibility = View.GONE
        connectButton.visibility = View.VISIBLE
    }

    override fun getRemoteUuid() = service?.getRemoteUuid()

    override fun showErrorWhileChoosingRandom() {
        Snackbar.make(view!!, R.string.error_choosing_random_partner, Snackbar.LENGTH_LONG).show()
    }

    override fun showNoOneAvailable() {
        Snackbar.make(view!!, R.string.msg_no_one_available, Snackbar.LENGTH_LONG).show()
    }

    override fun showLookingForPartnerMessage() {
        Snackbar.make(view!!, R.string.msg_looking_for_partner, Snackbar.LENGTH_SHORT).show()
    }

    override fun showOtherPartyFinished() {
        Snackbar.make(view!!, R.string.msg_other_party_finished, Snackbar.LENGTH_SHORT).show()
    }
}