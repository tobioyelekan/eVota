package com.example.evota.ui.confirmvote

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.example.evota.R
import com.example.evota.data.helpers.Status
import com.example.evota.data.model.VotingData
import com.example.evota.ui.BaseFragment
import com.example.evota.util.loadImage
import com.integratedbiometrics.ibscanultimate.*
import com.integratedbiometrics.ibscanultimate.IBScanDevice.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.confirm_vote_fragment.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

@AndroidEntryPoint
class ConfirmVoteFragment : BaseFragment(R.layout.confirm_vote_fragment), IBScanListener,
    IBScanDeviceListener {

    private val args: ConfirmVoteFragmentArgs by navArgs()

    private val viewModel: ConfirmVoteViewModel by viewModels()

    private val __TIMER_STATUS_DELAY__ = 500

    // Capture sequence definitions
    private val CAPTURE_SEQ_FLAT_SINGLE_FINGER = "Single flat finger"
    private val CAPTURE_SEQ_ROLL_SINGLE_FINGER = "Single rolled finger"
    private val CAPTURE_SEQ_2_FLAT_FINGERS = "2 flat fingers"
    private val CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS = "10 single flat fingers"
    private val CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS = "10 single rolled fingers"
    private val CAPTURE_SEQ_4_FLAT_FINGERS = "4 flat fingers"
    private val CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER =
        "10 flat fingers with 4-finger scanner"

    // Beep definitions
    private val __BEEP_FAIL__ = 0
    private val __BEEP_SUCCESS__ = 1
    private val __BEEP_OK__ = 2
    private val __BEEP_DEVICE_COMMUNICATION_BREAK__ = 3

    // LED color definitions
    private val __LED_COLOR_NONE__ = 0
    private val __LED_COLOR_GREEN__ = 1
    private val __LED_COLOR_RED__ = 2
    private val __LED_COLOR_YELLOW__ = 3

    // Key button definitions
    private val __LEFT_KEY_BUTTON__ = 1
    private val __RIGHT_KEY_BUTTON__ = 2

    /* *********************************************************************************************
	 * PRIVATE CLASSES
	 ******************************************************************************************** */
    /*
	 * This class wraps the data saved by the app for configuration changes.
	 */
    protected inner class AppData {
        /* The usb device currently selected. */
        var usbDevices = __INVALID_POS__

        /* The sequence of capture currently selected. */
        var captureSeq = __INVALID_POS__

        /* The current contents of the nfiq TextView. */
        var nfiq = __NFIQ_DEFAULT__

        /* The current contents of the frame time TextView. */
        var frameTime = __NA_DEFAULT__

        /* The current image displayed in the image preview ImageView. */
        var imageBitmap: Bitmap? = null

        /* The current background colors of the finger quality TextViews. */
        var fingerQualityColors = intArrayOf(
            FINGER_QUALITY_NOT_PRESENT_COLOR,
            FINGER_QUALITY_NOT_PRESENT_COLOR,
            FINGER_QUALITY_NOT_PRESENT_COLOR,
            FINGER_QUALITY_NOT_PRESENT_COLOR
        )

        /* Indicates whether the image preview ImageView can be long-clicked. */
        var imagePreviewImageClickable = false

        /* The current contents of the overlayText TextView. */
        var overlayText: String? = ""

        /* The current contents of the overlay color for overlayText TextView. */
        var overlayColor = PREVIEW_IMAGE_BACKGROUND

        /* The current contents of the status message TextView. */
        var statusMessage = __NA_DEFAULT__
    }

    inner class CaptureInfo {
        var PreCaptureMessage // to display on fingerprint window
                : String? = null
        var PostCaptuerMessage // to display on fingerprint window
                : String? = null
        var ImageType // capture mode
                : ImageType? = null
        var NumberOfFinger // number of finger count
                = 0
        var fingerName // finger name (e.g left thumbs, left index ... )
                : String? = null
    }

    /* *********************************************************************************************
	 * PRIVATE FIELDS
	 ******************************************************************************************** */
    /* 
	 * A handle to the single instance of the IBScan class that will be the primary interface to
	 * the library, for operations like getting the number of scanners (getDeviceCount()) and 
	 * opening scanners (openDeviceAsync()). 
	 */
    private var m_ibScan: IBScan? = null

    /* 
	 * A handle to the open IBScanDevice (if any) that will be the interface for getting data from
	 * the open scanner, including capturing the image (beginCaptureImage(), cancelCaptureImage()),
	 * and the type of image being captured.
	 */
    private var m_ibScanDevice: IBScanDevice? = null

    /*
	 * An object that will play a sound when the image capture has completed.
	 */
    private val m_beeper = PlaySound()

    /* 
	 * Information retained to show view.
	 */
    private var m_lastResultImage: ImageData? = null
    private var m_lastSegmentImages =
        arrayOfNulls<ImageData>(FINGER_SEGMENT_COUNT)

    /*
	 * Information retained for orientation changes.
	 */
    private val m_savedData = AppData()
    private var m_nSelectedDevIndex = -1 ///< Index of selected device
    private var m_bInitializing = false ///< Device initialization is in progress
    private var m_ImgSaveFolderName = ""
    var m_ImgSaveFolder = "" ///< Base folder for image saving
    var m_ImgSubFolder = "" ///< Sub Folder for image sequence
    private var m_strImageMessage: String? = ""
    private var m_bNeedClearPlaten = false
    private var m_bBlank = false
    private var m_bSaveWarningOfClearPlaten = false
    private var m_vecCaptureSeq =
        Vector<CaptureInfo>() ///< Sequence of capture steps
    private var m_nCurrentCaptureStep = -1 ///< Current capture step
    private var m_LedState: LedState? = null
    private var m_FingerQuality = arrayOf(
        FingerQualityState.FINGER_NOT_PRESENT,
        FingerQualityState.FINGER_NOT_PRESENT,
        FingerQualityState.FINGER_NOT_PRESENT,
        FingerQualityState.FINGER_NOT_PRESENT
    )
    private var m_ImageType: ImageType? = null
    private var m_nSegmentImageArrayCount = 0
    private var m_SegmentPositionArray: Array<SegmentPosition> = emptyArray()
    private var m_arrUsbDevices: ArrayList<String>? = null
    private var m_arrCaptureSeq: ArrayList<String>? = null
    private lateinit var m_drawBuffer: ByteArray
    private var m_scaleFactor = 0.0
    private var m_leftMargin = 0
    private var m_topMargin = 0
    private var m_minSDKVersion = ""
    private var m_bSpoofEnable = false
    private var m_SpoofThresLevel: String? = null
    lateinit var SpoofThresLevelAdapter: ArrayAdapter<CharSequence>

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // GLobal Varies Definitions
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    /* *********************************************************************************************
	 * INHERITED INTERFACE (Activity OVERRIDES)
	 ******************************************************************************************** */
    /*
     * Called when the activity is started.
     */

    private var m_BitmapImage: Bitmap? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        m_ibScan = IBScan.getInstance(requireContext().applicationContext)
        m_ibScan!!.setScanListener(this)
        val r = Resources.getSystem()
        val config = r.configuration
//        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            setContentView(R.layout.ib_scan_port)
//        } else {
//            setContentView(R.layout.ib_scan_land)
//        }

//        /* Initialize UI fields. */_InitUIFields()

        /*
		 * Make sure there are no USB devices attached that are IB scanners for which permission has
		 * not been granted.  For any that are found, request permission; we should receive a
		 * callback when permission is granted or denied and then when IBScan recognizes that new
		 * devices are connected, which will result in another refresh.
		 */
        val manager = requireContext().applicationContext
            .getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = manager.deviceList
        val deviceIterator: Iterator<UsbDevice> = deviceList.values.iterator()
        while (deviceIterator.hasNext()) {
            val device = deviceIterator.next()
            val isScanDevice = IBScan.isScanDevice(device)
            if (isScanDevice) {
                val hasPermission = manager.hasPermission(device)
                if (!hasPermission) {
                    m_ibScan!!.requestPermission(device.deviceId)
                }
            }
        }
        OnMsg_UpdateDeviceList(false)

//        /* Initialize UI with data. */_PopulateUI()
        val thread =
            _TimerTaskThreadCallback(__TIMER_STATUS_DELAY__)
        thread.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val (candidate1, candidate2) = args

        role1.text = candidate1.electionTitle
        partyLogo.loadImage(candidate1.party.logo)
        partyBrief.text = candidate1.party.code
        partyName.text = candidate1.party.name
        candidateImg.loadImage(candidate1.img)
        candidateName.text = candidate1.name

        role2.text = candidate2.electionTitle
        partyLogo2.loadImage(candidate2.party.logo)
        partyBrief2.text = candidate2.party.code
        partyName2.text = candidate2.party.name
        candidateImg2.loadImage(candidate2.img)
        candidateName2.text = candidate2.name

        m_btnCaptureStart.setOnClickListener(m_btnCaptureStartClickListener)
        m_btnCaptureStop.setOnClickListener(m_btnCaptureStopClickListener)

        m_cboSpoofThresLevel.onItemSelectedListener = m_cboSpoofThresLevelListener
        SpoofThresLevelAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.spoof_threslevel, android.R.layout.simple_spinner_item
        )
        SpoofThresLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        m_cboSpoofThresLevel.adapter = SpoofThresLevelAdapter
        m_cboSpoofThresLevel.setSelection(5) // Default Spoof level is 5

        viewModel.voter.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    showToastOnUiThread("Loading", Toast.LENGTH_LONG)
                }
                Status.SUCCESS -> {
                    it.data?.let { voterData ->
                        _SetStatusBarMessage(voterData.name)
                        activity?.runOnUiThread {
                            viewModel.voteNow(
                                listOf(
                                    VotingData(
                                        voterData.identification,
                                        args.candidate1.id,
                                        args.candidate1.electionId
                                    ),
                                    VotingData(
                                        voterData.identification,
                                        args.candidate2.id,
                                        args.candidate2.electionId
                                    )
                                )
                            )
                        }
                    }
                }
                Status.ERROR -> {
                    _SetStatusBarMessage(onError(it.message, it.throwable))
                }
            }
        })

        viewModel.votingNow.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    showToastOnUiThread("Voting...", Toast.LENGTH_LONG)
                }
                Status.SUCCESS -> {
                    it.data?.let { voteData ->
                        _SetStatusBarMessage(voteData.message)
                    }
                }
                Status.ERROR -> {
                    _SetStatusBarMessage(onError(it.message, it.throwable))
                }
            }
        })
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            setContentView(R.layout.ib_scan_port)
//        } else {
//            setContentView(R.layout.ib_scan_land)
//        }
//
//        /* Initialize UI fields for new orientation. */_InitUIFields()
//        OnMsg_UpdateDeviceList(true)
//
//        /* Populate UI with data from old orientation. */_PopulateUI()
//    }

    /*
	 * Release driver resources.
	 */
    override fun onDestroy() {
        super.onDestroy()
        for (i in 0..9) {
            try {
                _ReleaseDevice()
                break
            } catch (ibse: IBScanException) {
                if (ibse.type == IBScanException.Type.RESOURCE_LOCKED) {
                } else {
                    break
                }
            }
        }
    }

//    override fun onBackPressed() {
//        exitApp(this)
//    }
//
//    override fun onRetainNonConfigurationInstance(): Any {
//        return null
//    }

    /* *********************************************************************************************
	 * PRIVATE METHODS
	 ******************************************************************************************** */
    /*
	 * Initialize UI fields for new orientation.
	 */
//    private fun _InitUIFields() {
//        m_txtStatusMessage = findViewById<View>(R.id.txtStatusMessage) as TextView
//        m_txtOverlayText = findViewById<View>(R.id.txtOverlayText) as TextView
//
//        /* Hard-coded for four finger qualities. */
////		m_txtFingerQuality[0] = (TextView) findViewById(R.id.scan_states_color1);
////		m_txtFingerQuality[1] = (TextView) findViewById(R.id.scan_states_color2);
////		m_txtFingerQuality[2] = (TextView) findViewById(R.id.scan_states_color3);
////		m_txtFingerQuality[3] = (TextView) findViewById(R.id.scan_states_color4);
//
////		m_txtFrameTime        = (TextView) findViewById(R.id.frame_time);
//        m_imgPreview = findViewById<View>(R.id.imgPreview) as ImageView
//        m_imgPreview!!.setOnLongClickListener(m_imgPreviewLongClickListener)
//        m_imgPreview!!.setBackgroundColor(PREVIEW_IMAGE_BACKGROUND)
//        m_btnCaptureStop =
//            findViewById<View>(R.id.stop_capture_btn) as Button
//        m_btnCaptureStop!!.setOnClickListener(m_btnCaptureStopClickListener)
//        m_btnCaptureStart =
//            findViewById<View>(R.id.start_capture_btn) as Button
//        m_btnCaptureStart!!.setOnClickListener(m_btnCaptureStartClickListener)
//        m_cboUsbDevices = findViewById<View>(R.id.spinUsbDevices) as Spinner
//        m_cboCaptureSeq = findViewById<View>(R.id.spinCaptureSeq) as Spinner
//        m_chkSpoofEnable = findViewById<View>(R.id.chk_spoof) as CheckBox
//        m_chkSpoofEnable!!.setOnClickListener(m_chkEnableSpoofListener)
//        m_cboSpoofThresLevel = findViewById<View>(R.id.cbo_spoof_thres) as Spinner

//    }

    /*
	 * Populate UI with data from old orientation.
	 */
//    private fun _PopulateUI() {
//        if (m_savedData.usbDevices != __INVALID_POS__) {
//            m_cboUsbDevices!!.setSelection(m_savedData.usbDevices)
//        }
//        if (m_savedData.captureSeq != __INVALID_POS__) {
//            m_cboCaptureSeq!!.setSelection(m_savedData.captureSeq)
//        }
//        if (m_savedData.overlayText != null) {
//            m_txtOverlayText!!.setTextColor(m_savedData.overlayColor)
//            m_txtOverlayText!!.text = m_savedData.overlayText
//        }
//        if (m_savedData.imageBitmap != null) {
//            m_imgPreview!!.setImageBitmap(m_savedData.imageBitmap)
//        }
//        if (m_BitmapImage != null) {
//            m_BitmapImage!!.isRecycled
//        }
//        m_imgPreview!!.isLongClickable = m_savedData.imagePreviewImageClickable
//    }

    // Get IBScan.
    protected val iBScan: IBScan?
        protected get() = m_ibScan

    // Set IBScanDevice.
    // Get opened or null IBScanDevice.
    protected var iBScanDevice: IBScanDevice?
        protected get() = m_ibScanDevice
        protected set(ibScanDevice) {
            m_ibScanDevice = ibScanDevice
            ibScanDevice?.setScanDeviceListener(this)
        }

    /*
	 * Set status message text box.
	 */
    protected fun _SetStatusBarMessage(s: String?) {
        /* Make sure this occurs on the UI thread. */
        activity?.runOnUiThread { m_txtStatusMessage!!.text = s }
    }

    /*
	 * Set image overlay message text box.
	 */
    protected fun _SetOverlayText(s: String?, txtColor: Int) {
        m_savedData.overlayText = s
        m_savedData.overlayColor = txtColor

        /* Make sure this occurs on the UI thread. */
        activity?.runOnUiThread {
            m_txtOverlayText!!.text = s
        }
    }

    /*
	 * Timer task with using Thread
	 */
    internal inner class _TimerTaskThreadCallback(private val timeInterval: Int) :
        Thread() {
        override fun run() {
            while (!currentThread().isInterrupted) {
                if (iBScanDevice != null) {
//					OnMsg_DrawFingerQuality();
                    if (m_bNeedClearPlaten) m_bBlank = !m_bBlank
                }
                _Sleep(timeInterval)
                try {
                    sleep(timeInterval.toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

    }

    /*
	 * Initialize Device with using Thread
	 */
    internal inner class _InitializeDeviceThreadCallback(private val devIndex: Int) :
        Thread() {
        override fun run() {
            try {
                m_bInitializing = true
                val ibScanDeviceNew = iBScan!!.openDevice(devIndex)
                iBScanDevice = ibScanDeviceNew
                m_bInitializing = false
                if (ibScanDeviceNew != null) {
                    //getProperty device Width,Height
/*					String imageW = getIBScanDevice().getProperty(PropertyId.IMAGE_WIDTH);
					String imageH = getIBScanDevice().getProperty(PropertyId.IMAGE_HEIGHT);
					int	imageWidth = Integer.parseInt(imageW);
					int	imageHeight = Integer.parseInt(imageH);
//					m_BitmapImage = _CreateBitmap(imageWidth, imageHeight);
*/
                    val outWidth = m_imgPreview!!.width - 20
                    val outHeight = m_imgPreview!!.height - 20
                    m_BitmapImage =
                        Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    m_drawBuffer = ByteArray(outWidth * outHeight * 4)
                    m_LedState = iBScanDevice!!.operableLEDs
                    OnMsg_CaptureSeqStart()
                }
            } catch (ibse: IBScanException) {
                m_bInitializing = false
                if (ibse.type == IBScanException.Type.DEVICE_ACTIVE) {
                    _SetStatusBarMessage("[Error Code =-203] Device initialization failed because in use by another thread/process.")
                } else if (ibse.type == IBScanException.Type.USB20_REQUIRED) {
                    _SetStatusBarMessage("[Error Code =-209] Device initialization failed because SDK only works with USB 2.0.")
                } else if (ibse.type == IBScanException.Type.DEVICE_HIGHER_SDK_REQUIRED) {
                    try {
                        m_minSDKVersion = iBScan!!.getRequiredSDKVersion(devIndex)
                        _SetStatusBarMessage("[Error Code =-214] Devcie initialization failed because SDK Version $m_minSDKVersion is required at least.")
                    } catch (ibse1: IBScanException) {
                    }
                } else {
                    _SetStatusBarMessage("Device initialization failed.")
                }
                OnMsg_UpdateDisplayResources()
            }
        }

    }

    protected fun _CreateBitmap(width: Int, height: Int): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (bitmap != null) {
            val imageBuffer = ByteArray(width * height * 4)
            /* 
        	 * The image in the buffer is flipped vertically from what the Bitmap class expects;
        	 * we will flip it to compensate while moving it into the buffer.
		 */for (y in 0 until height) {
                for (x in 0 until width) {
                    imageBuffer[(y * width + x) * 4 + 2] = 128.toByte()
                    imageBuffer[(y * width + x) * 4 + 1] =
                        imageBuffer[(y * width + x) * 4 + 2]
                    imageBuffer[(y * width + x) * 4] = imageBuffer[(y * width + x) * 4 + 1]
                    imageBuffer[(y * width + x) * 4 + 3] = 255.toByte()
                }
            }
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer))
        }
        return bitmap
    }

    protected fun _CalculateScaleFactors(
        image: ImageData,
        outWidth: Int,
        outHeight: Int
    ) {
        var left = 0
        var top = 0
        var tmp_width = outWidth
        var tmp_height = outHeight
        val imgWidth = image.width
        val imgHeight = image.height
        var dispWidth: Int
        var dispHeight: Int
        var dispImgX: Int
        var dispImgY: Int
        if (outWidth > imgWidth) {
            tmp_width = imgWidth
            left = (outWidth - imgWidth) / 2
        }
        if (outHeight > imgHeight) {
            tmp_height = imgHeight
            top = (outHeight - imgHeight) / 2
        }
        val ratio_width = tmp_width.toFloat() / imgWidth.toFloat()
        val ratio_height = tmp_height.toFloat() / imgHeight.toFloat()
        dispWidth = outWidth
        dispHeight = outHeight
        if (ratio_width >= ratio_height) {
            dispWidth = tmp_height * imgWidth / imgHeight
            dispWidth -= dispWidth % 4
            dispHeight = tmp_height
            dispImgX = (tmp_width - dispWidth) / 2 + left
            dispImgY = top
        } else {
            dispWidth = tmp_width
            dispWidth -= dispWidth % 4
            dispHeight = tmp_width * imgHeight / imgWidth
            dispImgX = left
            dispImgY = (tmp_height - dispHeight) / 2 + top
        }
        if (dispImgX < 0) {
            dispImgX = 0
        }
        if (dispImgY < 0) {
            dispImgY = 0
        }

        ///////////////////////////////////////////////////////////////////////////////////
        m_scaleFactor = dispWidth.toDouble() / image.width
        m_leftMargin = dispImgX
        m_topMargin = dispImgY
        ///////////////////////////////////////////////////////////////////////////////////
    }

    protected fun _DrawOverlay_ImageText(canvas: Canvas?) {
/*
 * Draw text over bitmap image
 		Paint g = new Paint();
		g.setAntiAlias(true);
		if (m_bNeedClearPlaten)
			g.setColor(Color.RED);
		else
			g.setColor(Color.BLUE);
		g.setTypeface(Typeface.DEFAULT);
		g.setTextSize(20);
//		canvas.drawText(m_strImageMessage, 10, 20, g);
		canvas.drawText(m_strImageMessage, 20, 40, g);
*/

/*
 * Draw textview over imageview
 */
        if (m_bNeedClearPlaten) _SetOverlayText(
            m_strImageMessage,
            Color.RED
        ) else _SetOverlayText(m_strImageMessage, Color.BLUE)
    }

    protected fun _DrawOverlay_WarningOfClearPlaten(
        canvas: Canvas,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) {
        if (iBScanDevice == null) return
        val idle = !m_bInitializing && m_nCurrentCaptureStep == -1
        if (!idle && m_bNeedClearPlaten && m_bBlank) {
            val g = Paint()
            g.style = Paint.Style.STROKE
            g.color = Color.RED
            //			g.setStrokeWidth(10);
            g.strokeWidth = 20f
            g.isAntiAlias = true
            canvas.drawRect(
                left.toFloat(),
                top.toFloat(),
                width - 1.toFloat(),
                height - 1.toFloat(),
                g
            )
        }
    }

    protected fun _DrawOverlay_ResultSegmentImage(
        canvas: Canvas,
        image: ImageData,
        outWidth: Int,
        outHeight: Int
    ) {
        if (image.isFinal) {
//			if (m_chkDrawSegmentImage.isSelected())
            run {

                // Draw quadrangle for the segment image
                _CalculateScaleFactors(image, outWidth, outHeight)
                val g = Paint()
                g.color = Color.rgb(0, 128, 0)
                //				g.setStrokeWidth(1);
                g.strokeWidth = 4f
                g.isAntiAlias = true
                for (i in 0 until m_nSegmentImageArrayCount) {
                    val x1: Int =
                        m_leftMargin + (m_SegmentPositionArray[i].x1 * m_scaleFactor).toInt()
                    val x2: Int =
                        m_leftMargin + (m_SegmentPositionArray[i].x2 * m_scaleFactor).toInt()
                    val x3: Int =
                        m_leftMargin + (m_SegmentPositionArray[i].x3 * m_scaleFactor).toInt()
                    val x4: Int =
                        m_leftMargin + (m_SegmentPositionArray[i].x4 * m_scaleFactor).toInt()
                    val y1: Int =
                        m_topMargin + (m_SegmentPositionArray[i].y1 * m_scaleFactor).toInt()
                    val y2: Int =
                        m_topMargin + (m_SegmentPositionArray[i].y2 * m_scaleFactor).toInt()
                    val y3: Int =
                        m_topMargin + (m_SegmentPositionArray[i].y3 * m_scaleFactor).toInt()
                    val y4: Int =
                        m_topMargin + (m_SegmentPositionArray[i].y4 * m_scaleFactor).toInt()
                    canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), g)
                    canvas.drawLine(x2.toFloat(), y2.toFloat(), x3.toFloat(), y3.toFloat(), g)
                    canvas.drawLine(x3.toFloat(), y3.toFloat(), x4.toFloat(), y4.toFloat(), g)
                    canvas.drawLine(x4.toFloat(), y4.toFloat(), x1.toFloat(), y1.toFloat(), g)
                }
            }
        }
    }

    protected fun _DrawOverlay_RollGuideLine(
        canvas: Canvas,
        image: ImageData,
        width: Int,
        height: Int
    ) {
        if (iBScanDevice == null || m_nCurrentCaptureStep == -1) return
        if (m_ImageType == ImageType.ROLL_SINGLE_FINGER) {
            val g = Paint()
            val rollingdata: RollingData?
            g.isAntiAlias = true
            rollingdata = try {
                iBScanDevice!!.rollingInfo
            } catch (e: IBScanException) {
                null
            }
            if (rollingdata != null && rollingdata.rollingLineX > 0 &&
                (rollingdata.rollingState == RollingState.TAKE_ACQUISITION || rollingdata.rollingState == RollingState.COMPLETE_ACQUISITION)
            ) {
                _CalculateScaleFactors(image, width, height)
                val LineX = m_leftMargin + (rollingdata.rollingLineX * m_scaleFactor).toInt()

                // Guide line for rolling
                if (rollingdata.rollingState == RollingState.TAKE_ACQUISITION) g.color =
                    Color.RED else if (rollingdata.rollingState == RollingState.COMPLETE_ACQUISITION) g.color =
                    Color.GREEN
                if (rollingdata.rollingLineX > -1) {
//					g.setStrokeWidth(2);
                    g.strokeWidth = 4f
                    canvas.drawLine(LineX.toFloat(), 0f, LineX.toFloat(), height.toFloat(), g)
                }
            }
        }
    }

    protected fun _BeepFail() {
        try {
            val beeperType = iBScanDevice!!.operableBeeper
            if (beeperType != BeeperType.BEEPER_TYPE_NONE) {
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    12 /*300ms = 12*25ms*/,
                    0,
                    0
                )
                _Sleep(150)
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    6 /*150ms = 6*25ms*/,
                    0,
                    0
                )
                _Sleep(150)
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    6 /*150ms = 6*25ms*/,
                    0,
                    0
                )
                _Sleep(150)
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    6 /*150ms = 6*25ms*/,
                    0,
                    0
                )
            }
        } catch (ibse: IBScanException) {
            // devices for without beep chip
            // send the tone to the "alarm" stream (classic beeps go there) with 30% volume
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 30)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300) // 300 is duration in ms
            _Sleep(300 + 150)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150) // 150 is duration in ms
            _Sleep(150 + 150)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150) // 150 is duration in ms
            _Sleep(150 + 150)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150) // 150 is duration in ms
        }
    }

    protected fun _BeepSuccess() {
        try {
            val beeperType = iBScanDevice!!.operableBeeper
            if (beeperType != BeeperType.BEEPER_TYPE_NONE) {
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    4 /*100ms = 4*25ms*/,
                    0,
                    0
                )
                _Sleep(50)
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    4 /*100ms = 4*25ms*/,
                    0,
                    0
                )
            }
        } catch (ibse: IBScanException) {
            // devices for without beep chip
            // send the tone to the "alarm" stream (classic beeps go there) with 30% volume
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 30)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100) // 100 is duration in ms
            _Sleep(100 + 50)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100) // 100 is duration in ms
        }
    }

    protected fun _BeepOk() {
        try {
            val beeperType = iBScanDevice!!.operableBeeper
            if (beeperType != BeeperType.BEEPER_TYPE_NONE) {
                iBScanDevice!!.setBeeper(
                    BeepPattern.BEEP_PATTERN_GENERIC,
                    2 /*Sol*/,
                    4 /*100ms = 4*25ms*/,
                    0,
                    0
                )
            }
        } catch (ibse: IBScanException) {
            // devices for without beep chip
            // send the tone to the "alarm" stream (classic beeps go there) with 30% volume
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 30)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100) // 100 is duration in ms
        }
    }

    protected fun _BeepDeviceCommunicationBreak() {
        for (i in 0..7) {
            // send the tone to the "alarm" stream (classic beeps go there) with 30% volume
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 30)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100) // 100 is duration in ms
            _Sleep(100 + 100)
        }
    }

    protected fun _Sleep(time: Int) {
        try {
            Thread.sleep(time.toLong())
        } catch (e: InterruptedException) {
        }
    }

    protected fun _SetTxtNFIQScore(s: String?) {
//		this.m_savedData.nfiq = s;
//
//		/* Make sure this occurs on the UI thread. */
//		activity?.runOnUiThread(new Runnable()
//	{
//		@Override
//			public void run()
//			{
//				m_txtNFIQ.setText(s);
//			}
//		});
    }

    protected fun _SetImageMessage(s: String?) {
        m_strImageMessage = s
    }

    protected fun _AddCaptureSeqVector(
        PreCaptureMessage: String?, PostCaptuerMessage: String?,
        imageType: ImageType?, NumberOfFinger: Int, fingerName: String?
    ) {
        val info = CaptureInfo()
        info.PreCaptureMessage = PreCaptureMessage
        info.PostCaptuerMessage = PostCaptuerMessage
        info.ImageType = imageType
        info.NumberOfFinger = NumberOfFinger
        info.fingerName = fingerName
        m_vecCaptureSeq.addElement(info)
    }

    protected fun _UpdateCaptureSequences() {
        try {
            //store currently selected device
            var strSelectedText = ""
            val selectedSeq = m_cboCaptureSeq.selectedItemPosition
            if (selectedSeq > -1) strSelectedText = m_cboCaptureSeq.selectedItem.toString()

            // populate combo box
            m_arrCaptureSeq = ArrayList()
            m_arrCaptureSeq!!.add("- Please select -")
            val devIndex = m_cboUsbDevices.selectedItemPosition - 1
            if (devIndex > -1) {
                val devDesc = iBScan!!.getDeviceDescription(devIndex)
                if (devDesc.productName == "WATSON" ||
                    devDesc.productName == "WATSON MINI" ||
                    devDesc.productName == "SHERLOCK_ROIC" ||
                    devDesc.productName == "SHERLOCK"
                ) {
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_FLAT_SINGLE_FINGER)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_ROLL_SINGLE_FINGER)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_2_FLAT_FINGERS)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS)
                } else if (devDesc.productName == "COLUMBO" ||
                    devDesc.productName == "CURVE" ||
                    devDesc.productName == "DANNO"
                ) {
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_FLAT_SINGLE_FINGER)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS)
                } else if (devDesc.productName == "HOLMES" ||
                    devDesc.productName == "KOJAK" ||
                    devDesc.productName == "FIVE-0" ||
                    devDesc.productName == "TF10"
                ) {
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_FLAT_SINGLE_FINGER)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_ROLL_SINGLE_FINGER)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_2_FLAT_FINGERS)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_4_FLAT_FINGERS)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS)
                    m_arrCaptureSeq!!.add(CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER)
                }
            }
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_text_layout, m_arrCaptureSeq!!
            )

            m_cboCaptureSeq.adapter = adapter
            m_cboCaptureSeq.onItemSelectedListener = m_captureTypeItemSelectedListener
            if (m_arrCaptureSeq!!.size > 1) m_cboCaptureSeq.setSelection(1)


//			if (selectedSeq > -1)
//				this.m_cboCaptureSeq.setse(strSelectedText);
            OnMsg_UpdateDisplayResources()
        } catch (e: IBScanException) {
            e.printStackTrace()
        }
    }

    @Throws(IBScanException::class)
    protected fun _ReleaseDevice() {
        if (iBScanDevice != null) {
            if (iBScanDevice!!.isOpened == true) {
                iBScanDevice!!.close()
                iBScanDevice = null
            }
        }
        m_nCurrentCaptureStep = -1
        m_bInitializing = false
    }

    protected fun _SaveBitmapImage(image: ImageData?, fingerName: String?) {
/*		String filename = m_ImgSaveFolderName + ".bmp";

		try
	{
			image.saveToFile(filename, "BMP");
	}
		catch(IOException e)
	{
			e.printStackTrace();
	}
*/
    }

    protected fun _SaveWsqImage(image: ImageData, fingerName: String?) {
        val filename = "$m_ImgSaveFolderName.wsq"
        try {
            iBScanDevice!!.wsqEncodeToFile(
                filename,
                image.buffer,
                image.width,
                image.height,
                image.pitch,
                image.bitsPerPixel.toInt(),
                500,
                0.75,
                ""
            )
        } catch (e: IBScanException) {
            e.printStackTrace()
        }
    }

    protected fun _SavePngImage(image: ImageData, fingerName: String?) {
        val filename = "$m_ImgSaveFolderName.png"
        val file = File(filename)
        var filestream: FileOutputStream? = null
        try {
            filestream = FileOutputStream(file)
            val bitmap = image.toBitmap()
            bitmap.compress(CompressFormat.PNG, 100, filestream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                filestream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    protected fun _SaveJP2Image(image: ImageData?, fingerName: String?) {
/*		String filename = m_ImgSaveFolderName + ".jp2";

		try
	{
			getIBScanDevice().SaveJP2Image(filename, image.buffer, image.width, image.height, image.pitch, image.resolutionX, image.resolutionY , 80);
	}
		catch (IBScanException e)
	{
			e.printStackTrace();
	}
		catch( StackOverflowError e)
	{
			System.out.println("Exception :"+ e);
			e.printStackTrace();
	}
*/
    }

    fun _SetLEDs(
        info: CaptureInfo,
        ledColor: Int,
        bBlink: Boolean
    ) {
        try {
            val ledState = iBScanDevice!!.operableLEDs
            if (ledState.ledCount == 0) {
                return
            }
        } catch (ibse: IBScanException) {
            ibse.printStackTrace()
        }
        var setLEDs = 0
        if (ledColor == __LED_COLOR_NONE__) {
            try {
                iBScanDevice!!.leDs = setLEDs.toLong()
            } catch (ibse: IBScanException) {
                ibse.printStackTrace()
            }
            return
        }
        if (m_LedState!!.ledType == LedType.FSCAN) {
            if (bBlink) {
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_BLINK_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_BLINK_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_BLINK_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_BLINK_RED.toInt()
                    }
                }
            }
            if (info.ImageType == ImageType.ROLL_SINGLE_FINGER) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_ROLL.toInt()
            }
            if (info.fingerName == "SFF_Right_Thumb" ||
                info.fingerName == "SRF_Right_Thumb"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_TWO_THUMB.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Left_Thumb" ||
                info.fingerName == "SRF_Left_Thumb"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_TWO_THUMB.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_RED.toInt()
                    }
                }
            } else if (info.fingerName == "TFF_2_Thumbs") {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_TWO_THUMB.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_THUMB_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_THUMB_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Left_Index" ||
                info.fingerName == "SRF_Left_Index"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_LEFT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Left_Middle" ||
                info.fingerName == "SRF_Left_Middle"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_LEFT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Left_Ring" ||
                info.fingerName == "SRF_Left_Ring"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_LEFT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Left_Little" ||
                info.fingerName == "SRF_Left_Little"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_LEFT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_RED.toInt()
                    }
                }
            } else if (info.fingerName == "4FF_Left_4_Fingers") {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_LEFT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_INDEX_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_MIDDLE_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_RING_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_LEFT_LITTLE_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Right_Index" ||
                info.fingerName == "SRF_Right_Index"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_RIGHT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Right_Middle" ||
                info.fingerName == "SRF_Right_Middle"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_RIGHT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Right_Ring" ||
                info.fingerName == "SRF_Right_Ring"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_RIGHT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_RED.toInt()
                    }
                }
            } else if (info.fingerName == "SFF_Right_Little" ||
                info.fingerName == "SRF_Right_Little"
            ) {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_RIGHT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_RED.toInt()
                    }
                }
            } else if (info.fingerName == "4FF_Right_4_Fingers") {
                setLEDs = setLEDs or IBSU_LED_F_PROGRESS_RIGHT_HAND.toInt()
                when (ledColor) {
                    __LED_COLOR_GREEN__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_GREEN.toInt()
                    }
                    __LED_COLOR_RED__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_RED.toInt()
                    }
                    __LED_COLOR_YELLOW__ -> {
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_GREEN.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_INDEX_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_MIDDLE_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_RING_RED.toInt()
                        setLEDs = setLEDs or IBSU_LED_F_RIGHT_LITTLE_RED.toInt()
                    }
                }
            }
            if (ledColor == __LED_COLOR_NONE__) {
                setLEDs = 0
            }
            try {
                iBScanDevice!!.leDs = setLEDs.toLong()
            } catch (ibse: IBScanException) {
                ibse.printStackTrace()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Event-dispatch threads
    private fun OnMsg_SetStatusBarMessage(s: String) {
        activity?.runOnUiThread { _SetStatusBarMessage(s) }
    }

    private fun OnMsg_SetTxtNFIQScore(s: String) {
        activity?.runOnUiThread { _SetTxtNFIQScore(s) }
    }

    private fun OnMsg_Beep(beepType: Int) {
        activity?.runOnUiThread { if (beepType == __BEEP_FAIL__) _BeepFail() else if (beepType == __BEEP_SUCCESS__) _BeepSuccess() else if (beepType == __BEEP_OK__) _BeepOk() else if (beepType == __BEEP_DEVICE_COMMUNICATION_BREAK__) _BeepDeviceCommunicationBreak() }
    }

    private fun OnMsg_CaptureSeqStart() {
        activity?.runOnUiThread(Runnable {
            if (iBScanDevice == null) {
                OnMsg_UpdateDisplayResources()
                return@Runnable
            }
            var strCaptureSeq = ""
            val nSelectedSeq = m_cboCaptureSeq.selectedItemPosition
            if (nSelectedSeq > -1) strCaptureSeq = m_cboCaptureSeq.selectedItem.toString()
            m_vecCaptureSeq.clear()
            /** Please refer to definition below
             * protected final String CAPTURE_SEQ_FLAT_SINGLE_FINGER 				= "Single flat finger";
             * protected final String CAPTURE_SEQ_ROLL_SINGLE_FINGER 				= "Single rolled finger";
             * protected final String CAPTURE_SEQ_2_FLAT_FINGERS 					= "2 flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS 			= "10 single flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS 		= "10 single rolled fingers";
             * protected final String CAPTURE_SEQ_4_FLAT_FINGERS 					= "4 flat fingers";
             * protected final String CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER 	= "10 flat fingers with 4-finger scanner";
             */
            /** Please refer to definition below
             * protected final String CAPTURE_SEQ_FLAT_SINGLE_FINGER 				= "Single flat finger";
             * protected final String CAPTURE_SEQ_ROLL_SINGLE_FINGER 				= "Single rolled finger";
             * protected final String CAPTURE_SEQ_2_FLAT_FINGERS 					= "2 flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS 			= "10 single flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS 		= "10 single rolled fingers";
             * protected final String CAPTURE_SEQ_4_FLAT_FINGERS 					= "4 flat fingers";
             * protected final String CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER 	= "10 flat fingers with 4-finger scanner";
             */
            /** Please refer to definition below
             * protected final String CAPTURE_SEQ_FLAT_SINGLE_FINGER 				= "Single flat finger";
             * protected final String CAPTURE_SEQ_ROLL_SINGLE_FINGER 				= "Single rolled finger";
             * protected final String CAPTURE_SEQ_2_FLAT_FINGERS 					= "2 flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS 			= "10 single flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS 		= "10 single rolled fingers";
             * protected final String CAPTURE_SEQ_4_FLAT_FINGERS 					= "4 flat fingers";
             * protected final String CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER 	= "10 flat fingers with 4-finger scanner";
             */
            /** Please refer to definition below
             * protected final String CAPTURE_SEQ_FLAT_SINGLE_FINGER 				= "Single flat finger";
             * protected final String CAPTURE_SEQ_ROLL_SINGLE_FINGER 				= "Single rolled finger";
             * protected final String CAPTURE_SEQ_2_FLAT_FINGERS 					= "2 flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS 			= "10 single flat fingers";
             * protected final String CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS 		= "10 single rolled fingers";
             * protected final String CAPTURE_SEQ_4_FLAT_FINGERS 					= "4 flat fingers";
             * protected final String CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER 	= "10 flat fingers with 4-finger scanner";
             */
            if (strCaptureSeq == CAPTURE_SEQ_FLAT_SINGLE_FINGER) {
                _AddCaptureSeqVector(
                    "Please put a single finger on the sensor!",
                    "Keep finger on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Unknown"
                )
            }
            if (strCaptureSeq == CAPTURE_SEQ_ROLL_SINGLE_FINGER) {
                _AddCaptureSeqVector(
                    "Please put a single finger on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SRF_Unknown"
                )
            }
            if (strCaptureSeq === CAPTURE_SEQ_2_FLAT_FINGERS) {
                _AddCaptureSeqVector(
                    "Please put two fingers on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_TWO_FINGERS,
                    2,
                    "TFF_Unknown"
                )
            }
            if (strCaptureSeq === CAPTURE_SEQ_10_SINGLE_FLAT_FINGERS) {
                _AddCaptureSeqVector(
                    "Please put right thumb on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Right_Thumb"
                )
                _AddCaptureSeqVector(
                    "Please put right index on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Right_Index"
                )
                _AddCaptureSeqVector(
                    "Please put right middle on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Right_Middle"
                )
                _AddCaptureSeqVector(
                    "Please put right ring on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Right_Ring"
                )
                _AddCaptureSeqVector(
                    "Please put right little on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Right_Little"
                )
                _AddCaptureSeqVector(
                    "Please put left thumb on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Left_Thumb"
                )
                _AddCaptureSeqVector(
                    "Please put left index on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Left_Index"
                )
                _AddCaptureSeqVector(
                    "Please put left middle on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Left_Middle"
                )
                _AddCaptureSeqVector(
                    "Please put left ring on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Left_Ring"
                )
                _AddCaptureSeqVector(
                    "Please put left little on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_SINGLE_FINGER,
                    1,
                    "SFF_Left_Little"
                )
            }
            if (strCaptureSeq === CAPTURE_SEQ_10_SINGLE_ROLLED_FINGERS) {
                _AddCaptureSeqVector(
                    "Please put right thumb on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Right_Thumb"
                )
                _AddCaptureSeqVector(
                    "Please put right index on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Right_Index"
                )
                _AddCaptureSeqVector(
                    "Please put right middle on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Right_Middle"
                )
                _AddCaptureSeqVector(
                    "Please put right ring on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Right_Ring"
                )
                _AddCaptureSeqVector(
                    "Please put right little on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Right_Little"
                )
                _AddCaptureSeqVector(
                    "Please put left thumb on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Left_Thumb"
                )
                _AddCaptureSeqVector(
                    "Please put left index on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Left_Index"
                )
                _AddCaptureSeqVector(
                    "Please put left middle on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Left_Middle"
                )
                _AddCaptureSeqVector(
                    "Please put left ring on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Left_Ring"
                )
                _AddCaptureSeqVector(
                    "Please put left little on the sensor!",
                    "Roll finger!",
                    ImageType.ROLL_SINGLE_FINGER,
                    1,
                    "SFF_Left_Little"
                )
            }
            if (strCaptureSeq === CAPTURE_SEQ_4_FLAT_FINGERS) {
                _AddCaptureSeqVector(
                    "Please put 4 fingers on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_FOUR_FINGERS,
                    4,
                    "4FF_Unknown"
                )
            }
            if (strCaptureSeq === CAPTURE_SEQ_10_FLAT_WITH_4_FINGER_SCANNER) {
                _AddCaptureSeqVector(
                    "Please put right 4-fingers on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_FOUR_FINGERS,
                    4,
                    "4FF_Right_4_Fingers"
                )
                _AddCaptureSeqVector(
                    "Please put left 4-fingers on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_FOUR_FINGERS,
                    4,
                    "4FF_Left_4_Fingers"
                )
                _AddCaptureSeqVector(
                    "Please put 2-thumbs on the sensor!",
                    "Keep fingers on the sensor!",
                    ImageType.FLAT_TWO_FINGERS,
                    2,
                    "TFF_2_Thumbs"
                )
            }
            OnMsg_CaptureSeqNext()
        })
    }

    private fun OnMsg_CaptureSeqNext() {
        activity?.runOnUiThread(Runnable {
            if (iBScanDevice == null) return@Runnable
            m_bBlank = false
            for (i in 0..3) m_FingerQuality[i] = FingerQualityState.FINGER_NOT_PRESENT
            m_nCurrentCaptureStep++
            if (m_nCurrentCaptureStep >= m_vecCaptureSeq.size) {
                // All of capture sequence completely
                val tmpInfo = CaptureInfo()
                _SetLEDs(tmpInfo, __LED_COLOR_NONE__, false)
                m_nCurrentCaptureStep = -1
                OnMsg_UpdateDisplayResources()
                return@Runnable
            }
            try {
                /*					if (m_chkDetectSmear.isSelected())
                                    {
                                        getIBScanDevice().setProperty(IBScanDevice.PropertyId.ROLL_MODE, "1");
                                        String strValue = String.valueOf(m_cboSmearLevel.getSelectedIndex());
                                        getIBScanDevice().setProperty(IBScanDevice.PropertyId.ROLL_LEVEL, strValue);
                            }
                            else
                            {
                                        getIBScanDevice().setProperty(IBScanDevice.PropertyId.ROLL_MODE, "0");
                                    }
                */
                // Check spoof enabled
                val result_str =
                    iBScanDevice!!.getProperty(PropertyId.IS_SPOOF_SUPPORTED)
                if (m_bSpoofEnable == true) {
                    if (result_str == "TRUE") // Check device is support Spoof function
                    {
                        try {
                            iBScanDevice!!.setProperty(
                                PropertyId.ENABLE_SPOOF,
                                "TRUE"
                            )
                            iBScanDevice!!.setProperty(
                                PropertyId.SPOOF_LEVEL,
                                m_SpoofThresLevel
                            )
                        } catch (e: IBScanException) {
                            showToastOnUiThread("Spoof library not found", Toast.LENGTH_SHORT)
                        }
                    } else  // Not support case
                    {
                        showToastOnUiThread(
                            "This scanner doesn't support spoofing",
                            Toast.LENGTH_SHORT
                        )
                    }
                } else  // Spoof disabled
                {
                    if (result_str == "TRUE") // Check device is support Spoof function
                    {
                        try {
                            iBScanDevice!!.setProperty(
                                PropertyId.ENABLE_SPOOF,
                                "FALSE"
                            )
                        } catch (e: IBScanException) {
                            //showToastOnUiThread("Spoof library not found", Toast.LENGTH_SHORT);
                        }
                    }
                }
                // Make capture delay for display result image on multi capture mode (500 ms)
                if (m_nCurrentCaptureStep > 0) {
                    _Sleep(500)
                    m_strImageMessage = ""
                }
                val info =
                    m_vecCaptureSeq.elementAt(m_nCurrentCaptureStep)
                val imgRes = ImageResolution.RESOLUTION_500
                val bAvailable =
                    iBScanDevice!!.isCaptureAvailable(info.ImageType, imgRes)
                if (!bAvailable) {
                    _SetStatusBarMessage("The capture mode (" + info.ImageType + ") is not available")
                    m_nCurrentCaptureStep = -1
                    OnMsg_UpdateDisplayResources()
                    return@Runnable
                }

                // Start capture
                var captureOptions = 0
                //					if (m_chkAutoContrast.isSelected())
                captureOptions = captureOptions or OPTION_AUTO_CONTRAST
                //					if (m_chkAutoCapture.isSelected())
                captureOptions = captureOptions or OPTION_AUTO_CAPTURE
                //					if (m_chkIgnoreFingerCount.isSelected())
                captureOptions = captureOptions or OPTION_IGNORE_FINGER_COUNT
                iBScanDevice!!.beginCaptureImage(info.ImageType, imgRes, captureOptions)
                val strMessage = info.PreCaptureMessage
                _SetStatusBarMessage(strMessage)
                //					if (!m_chkAutoCapture.isSelected())
                //						strMessage += "\r\nPress button 'Take Result Image' when image is good!";
                _SetImageMessage(strMessage)
                m_strImageMessage = strMessage
                m_ImageType = info.ImageType
                _SetLEDs(info, __LED_COLOR_RED__, true)
                OnMsg_UpdateDisplayResources()
            } catch (ibse: IBScanException) {
                ibse.printStackTrace()
                _SetStatusBarMessage("Failed to execute beginCaptureImage()")
                m_nCurrentCaptureStep = -1
            }
        })
    }

    private fun OnMsg_cboUsbDevice_Changed() {
        activity?.runOnUiThread(Runnable {
            if (m_nSelectedDevIndex == m_cboUsbDevices.selectedItemPosition) return@Runnable
            m_nSelectedDevIndex = m_cboUsbDevices.selectedItemPosition
            if (iBScanDevice != null) {
                try {
                    _ReleaseDevice()
                } catch (ibse: IBScanException) {
                    ibse.printStackTrace()
                }
            }
            _UpdateCaptureSequences()
        })
    }

    private fun OnMsg_UpdateDeviceList(bConfigurationChanged: Boolean) {
        activity?.runOnUiThread {
            try {
                val idle =
                    !m_bInitializing && m_nCurrentCaptureStep == -1 ||
                            bConfigurationChanged
                if (idle) {
                    m_btnCaptureStop.isEnabled = false
                    m_btnCaptureStart.isEnabled = false
                }

                //store currently selected device
                var strSelectedText = ""
                var selectedDev = m_cboUsbDevices.selectedItemPosition
                if (selectedDev > -1) strSelectedText =
                    m_cboUsbDevices.selectedItem.toString()
                m_arrUsbDevices = ArrayList()
                m_arrUsbDevices!!.add("- Please select -")
                // populate combo box
                val devices = iBScan!!.deviceCount
                //					setDeviceCount(devices);
                //					m_cboUsbDevices.setMaximumRowCount(devices + 1);
                selectedDev = 0
                for (i in 0 until devices) {
                    val devDesc = iBScan!!.getDeviceDescription(i)
                    var strDevice: String
                    strDevice =
                        devDesc.productName + "_v" + devDesc.fwVersion + "(" + devDesc.serialNumber + ")"
                    m_arrUsbDevices!!.add(strDevice)
                    if (strDevice === strSelectedText) selectedDev = i + 1
                }
                val adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_text_layout, m_arrUsbDevices!!
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                m_cboUsbDevices.adapter = adapter
                m_cboUsbDevices.onItemSelectedListener = m_cboUsbDevicesItemSelectedListener
                if (selectedDev == 0 && m_cboUsbDevices.count == 2) selectedDev = 1
                m_cboUsbDevices.setSelection(selectedDev)
                if (idle) {
                    OnMsg_cboUsbDevice_Changed()
                    _UpdateCaptureSequences()
                }
            } catch (e: IBScanException) {
                e.printStackTrace()
            }
        }
    }

    private fun OnMsg_UpdateDisplayResources() {
        activity?.runOnUiThread {
            val selectedDev = m_cboUsbDevices.selectedItemPosition > 0
            val captureSeq = m_cboCaptureSeq.selectedItemPosition > 0
            val idle = !m_bInitializing && m_nCurrentCaptureStep == -1
            val active = !m_bInitializing && m_nCurrentCaptureStep != -1
            val uninitializedDev = selectedDev && iBScanDevice == null
            m_cboUsbDevices.isEnabled = idle
            m_cboCaptureSeq.isEnabled = selectedDev && idle
            m_btnCaptureStart.isEnabled = captureSeq
            m_btnCaptureStop.isEnabled = active

            //				m_chkAutoContrast.setEnabled(selectedDev && idle );
            //				m_chkAutoCapture.setEnabled(selectedDev && idle );
            //				m_chkIgnoreFingerCount.setEnabled(selectedDev && idle );
            //				m_chkSaveImages.setEnabled(selectedDev && idle );
            //				m_btnImageFolder.setEnabled(selectedDev && idle );

            //				m_chkUseClearPlaten.setEnabled(uninitializedDev);
            if (active) {
                m_btnCaptureStart.text = "Take Result Image"
            } else {
                m_btnCaptureStart.text = "Start"
            }
        }
    }

    private fun OnMsg_AskRecapture(imageStatus: IBScanException) {
        activity?.runOnUiThread {
            val askMsg: String
            askMsg =
                "[Warning = " + imageStatus.type.toString() + "] Do you want a recapture?"
            val dlgAskRecapture =
                AlertDialog.Builder(requireContext())
            dlgAskRecapture.setMessage(askMsg)
            dlgAskRecapture.setPositiveButton(
                "Yes"
            ) { dialog, which -> // To recapture current finger position
                m_nCurrentCaptureStep--
                OnMsg_CaptureSeqNext()
            }
            dlgAskRecapture.setNegativeButton(
                "No"
            ) { dialog, which -> OnMsg_CaptureSeqNext() }
            dlgAskRecapture.show()
        }
    }

    private fun OnMsg_DeviceCommunicationBreak() {
        activity?.runOnUiThread(Runnable {
            if (iBScanDevice == null) return@Runnable
            _SetStatusBarMessage("Device communication was broken")
            try {
                _ReleaseDevice()
                OnMsg_Beep(__BEEP_DEVICE_COMMUNICATION_BREAK__)
                OnMsg_UpdateDeviceList(false)
            } catch (ibse: IBScanException) {
                if (ibse.type == IBScanException.Type.RESOURCE_LOCKED) {
                    OnMsg_DeviceCommunicationBreak()
                }
            }
        })
    }

    private fun OnMsg_DrawImage(device: IBScanDevice, image: ImageData) {
        activity?.runOnUiThread(Runnable {
            val destWidth = m_imgPreview!!.width - 20
            val destHeight = m_imgPreview!!.height - 20
            //				int outImageSize = destWidth * destHeight;
            try {
                if (destHeight <= 0 || destWidth <= 0) return@Runnable
                if (destWidth != m_BitmapImage!!.width || destHeight != m_BitmapImage!!.height) {
                    // if image size is changed (e.g changed capture type from flat to rolled finger)
                    // Create bitmap again
                    m_BitmapImage =
                        Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
                    m_drawBuffer = ByteArray(destWidth * destHeight * 4)
                }
                if (image.isFinal) {
                    iBScanDevice!!.generateDisplayImage(
                        image.buffer,
                        image.width,
                        image.height,
                        m_drawBuffer,
                        destWidth,
                        destHeight,
                        255.toByte(),
                        2 /*IBSU_IMG_FORMAT_RGB32*/,
                        2 /*HIGH QUALITY*/,
                        true
                    )
                    /*						for (int i=0; i<destWidth*destHeight; i++)
                        {
                                if (m_drawBuffer[i] != -1) {
                                    OnMsg_Beep(__BEEP_OK__);
                            break;
                        }
                            }
    */
                } else {
                    iBScanDevice!!.generateDisplayImage(
                        image.buffer,
                        image.width,
                        image.height,
                        m_drawBuffer,
                        destWidth,
                        destHeight,
                        255.toByte(),
                        2 /*IBSU_IMG_FORMAT_RGB32*/,
                        0 /*LOW QUALITY*/,
                        true
                    )
                }
            } catch (e: IBScanException) {
                e.printStackTrace()
            }
            m_BitmapImage!!.copyPixelsFromBuffer(ByteBuffer.wrap(m_drawBuffer))
            // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            val canvas = Canvas(m_BitmapImage!!)
            _DrawOverlay_ImageText(canvas)
            _DrawOverlay_WarningOfClearPlaten(canvas, 0, 0, destWidth, destHeight)
            _DrawOverlay_ResultSegmentImage(canvas, image, destWidth, destHeight)
            _DrawOverlay_RollGuideLine(canvas, image, destWidth, destHeight)
            /*				_DrawOverlay_WarningOfClearPlaten(canvas, 0, 0, image.width, image.height);
                    _DrawOverlay_ResultSegmentImage(canvas, image, image.width, image.height);
                    _DrawOverlay_RollGuideLine(canvas, image, image.width, image.height);
                 */m_savedData.imageBitmap = m_BitmapImage
            m_imgPreview!!.setImageBitmap(m_BitmapImage)
        })
    }

    /*
	 * Show Toast message on UI thread.
	 */
    private fun showToastOnUiThread(message: String, duration: Int) {
        activity?.runOnUiThread {
            val toast = Toast.makeText(requireContext(), message, duration)
            toast.show()
        }
    }

    private fun showEnlargedImage() {
        /*
		 * Sanity check.  Make sure the image exists.
		 */
//        if (m_lastResultImage == null) {
//            showToastOnUiThread("No last image information", Toast.LENGTH_SHORT)
//            return
//        }
//        m_enlargedDialog = Dialog(this, R.style.Enlarged)
//        m_enlargedDialog.setContentView(R.layout.enlarged)
//        m_enlargedDialog!!.setCancelable(false)
//        val bitmap = m_lastResultImage!!.toBitmap()
//        m_imgEnlargedView =
//            m_enlargedDialog!!.findViewById<View>(R.id.enlarged_image) as ImageView
//        m_btnCloseEnlargedDialog =
//            m_enlargedDialog!!.findViewById<View>(R.id.btnClose) as Button
//        m_txtEnlargedScale =
//            m_enlargedDialog!!.findViewById<View>(R.id.txtDisplayImgScale) as TextView
//        m_imgEnlargedView!!.scaleType = ImageView.ScaleType.CENTER
//        m_imgEnlargedView!!.setImageBitmap(bitmap)
//        m_btnCloseEnlargedDialog!!.setOnClickListener(m_btnCloseEnlargedDialogClickListener)
//        val mAttacher = PhotoViewAttacher(m_imgEnlargedView)
//        val display = windowManager.defaultDisplay
//        val size = Point()
//        display.getSize(size)
//        val disp_w = size.x - 20 //m_imgEnlargedView.getWidth();
//        val disp_h = size.y - 50 //m_imgEnlargedView.getHeight();
//        val ratio_w = disp_w.toFloat() / m_lastResultImage!!.width
//        val ratio_h = disp_h.toFloat() / m_lastResultImage!!.height
//        var ratio_1x = 0.0f
//        ratio_1x = if (ratio_w > ratio_h) {
//            m_lastResultImage!!.height.toFloat() / disp_h
//        } else {
//            m_lastResultImage!!.width.toFloat() / disp_w
//        }
//        mAttacher.maximumScale = ratio_1x * 8
//        mAttacher.mediumScale = ratio_1x * 4
//        mAttacher.minimumScale = ratio_1x
//        val zoom_1x = ratio_1x
//        mAttacher.setOnMatrixChangeListener {
//            m_txtEnlargedScale!!.text = String.format(
//                "Scale : %1$.1f x",
//                mAttacher.scale / zoom_1x
//            )
//        }
//        m_imgEnlargedView!!.post { mAttacher.setScale(zoom_1x, false) }
//        m_enlargedDialog!!.show()
    }

    /*
	 * Compress the image and attach it to an e-mail using an installed e-mail client. 
	 */
//    private fun sendImageInEmail(imageData: ImageData, fileName: String) {
//        var created = false
//        val ur: ArrayList<*> = ArrayList<Any?>()
//        try {
//            val MainPNGfilename =
//                Environment.getExternalStorageDirectory()
//                    .path + "/" + fileName + ".png"
//            val MainBMPfilename =
//                Environment.getExternalStorageDirectory()
//                    .path + "/" + fileName + ".bmp"
//            val MainJP2filename =
//                Environment.getExternalStorageDirectory()
//                    .path + "/" + fileName + ".jp2"
//            val MainWSQfilename =
//                Environment.getExternalStorageDirectory()
//                    .path + "/" + fileName + ".wsq"
//            iBScanDevice!!.SavePngImage(
//                MainPNGfilename,
//                imageData.buffer,
//                imageData.width,
//                imageData.height,
//                imageData.pitch,
//                imageData.resolutionX,
//                imageData.resolutionY
//            )
//            iBScanDevice!!.SaveBitmapImage(
//                MainBMPfilename,
//                imageData.buffer,
//                imageData.width,
//                imageData.height,
//                imageData.pitch,
//                imageData.resolutionX,
//                imageData.resolutionY
//            )
//            iBScanDevice!!.SaveJP2Image(
//                MainJP2filename,
//                imageData.buffer,
//                imageData.width,
//                imageData.height,
//                imageData.pitch,
//                imageData.resolutionX,
//                imageData.resolutionY,
//                80
//            )
//            iBScanDevice!!.wsqEncodeToFile(
//                MainWSQfilename,
//                imageData.buffer,
//                imageData.width,
//                imageData.height,
//                imageData.pitch,
//                imageData.bitsPerPixel.toInt(),
//                500,
//                0.75,
//                ""
//            )
//            ur.add(Uri.fromFile(File(MainPNGfilename)))
//            ur.add(Uri.fromFile(File(MainBMPfilename)))
//            ur.add(Uri.fromFile(File(MainJP2filename)))
//            ur.add(Uri.fromFile(File(MainWSQfilename)))
//            for (i in 0 until m_nSegmentImageArrayCount) {
//                val SegmentPNGfilename =
//                    Environment.getExternalStorageDirectory()
//                        .path + "/segment_" + i + "_" + fileName + ".png"
//                val SegmentBMPfilename =
//                    Environment.getExternalStorageDirectory()
//                        .path + "/segment_" + i + "_" + fileName + ".bmp"
//                val SegmentJP2filename =
//                    Environment.getExternalStorageDirectory()
//                        .path + "/segment_" + i + "_" + fileName + ".jp2"
//                val SegmentWSQfilename =
//                    Environment.getExternalStorageDirectory()
//                        .path + "/segment_" + i + "_" + fileName + ".wsq"
//                try {
//                    iBScanDevice!!.SavePngImage(
//                        SegmentPNGfilename,
//                        m_lastSegmentImages[i]!!.buffer,
//                        m_lastSegmentImages[i]!!.width,
//                        m_lastSegmentImages[i]!!.height,
//                        m_lastSegmentImages[i]!!.pitch,
//                        m_lastSegmentImages[i]!!.resolutionX,
//                        m_lastSegmentImages[i]!!.resolutionY
//                    )
//                    iBScanDevice!!.SaveBitmapImage(
//                        SegmentBMPfilename,
//                        m_lastSegmentImages[i]!!.buffer,
//                        m_lastSegmentImages[i]!!.width,
//                        m_lastSegmentImages[i]!!.height,
//                        m_lastSegmentImages[i]!!.pitch,
//                        m_lastSegmentImages[i]!!.resolutionX,
//                        m_lastSegmentImages[i]!!.resolutionY
//                    )
//                    iBScanDevice!!.SaveJP2Image(
//                        SegmentJP2filename,
//                        m_lastSegmentImages[i]!!.buffer,
//                        m_lastSegmentImages[i]!!.width,
//                        m_lastSegmentImages[i]!!.height,
//                        m_lastSegmentImages[i]!!.pitch,
//                        m_lastSegmentImages[i]!!.resolutionX,
//                        m_lastSegmentImages[i]!!.resolutionY,
//                        80
//                    )
//                    iBScanDevice!!.wsqEncodeToFile(
//                        SegmentWSQfilename,
//                        m_lastSegmentImages[i]!!.buffer,
//                        m_lastSegmentImages[i]!!.width,
//                        m_lastSegmentImages[i]!!.height,
//                        m_lastSegmentImages[i]!!.pitch,
//                        m_lastSegmentImages[i]!!.bitsPerPixel.toInt(),
//                        500,
//                        0.75,
//                        ""
//                    )
//                    ur.add(Uri.fromFile(File(SegmentPNGfilename)))
//                    ur.add(Uri.fromFile(File(SegmentBMPfilename)))
//                    ur.add(Uri.fromFile(File(SegmentJP2filename)))
//                    ur.add(Uri.fromFile(File(SegmentWSQfilename)))
//                } catch (e: IBScanException) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Could not create image for e-mail",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//            created = true
//        } catch (e: IBScanException) {
//            Toast.makeText(
//                requireContext(),
//                "Could not create image for e-mail",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//
//
//        /* If file was created, send the e-mail. */if (created) {
//            attachAndSendEmail(ur, "Fingerprint Image", fileName)
//        }
//    }

    /*
	 * Attach file to e-mail and send.
	 */
//    private fun attachAndSendEmail(
//        ur: ArrayList<*>,
//        subject: String,
//        message: String
//    ) {
//        val i = Intent(Intent.ACTION_SEND_MULTIPLE)
//        i.type = "message/rfc822"
//        i.putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
//        i.putExtra(Intent.EXTRA_SUBJECT, subject)
//        //		i.putExtra(Intent.EXTRA_STREAM,  ur);
//        i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ur)
//        i.putExtra(Intent.EXTRA_TEXT, message)
//        try {
//            startActivity(Intent.createChooser(i, "Send mail..."))
//        } catch (anfe: ActivityNotFoundException) {
//            showToastOnUiThread("There are no e-mail clients installed", Toast.LENGTH_LONG)
//        }
//    }

    /*
	 * Prompt to send e-mail with image.
	 */
//    private fun promptForEmail(imageData: ImageData) {
//        /* The dialog must be shown from the UI thread. */
//        activity?.runOnUiThread {
//            val inflater = layoutInflater
//            val fileNameView: View =
//                inflater.inflate(R.layout.file_name_dialog, null)
//            val builder =
//                AlertDialog.Builder(this@SimpleScanActivity)
//                    .setView(fileNameView)
//                    .setTitle("Enter file name")
//                    .setPositiveButton(
//                        "OK"
//                    ) { dialog, which ->
//                        val text =
//                            fileNameView.findViewById<View>(R.id.file_name) as EditText
//                        val fileName = text.text.toString()
//
//                        /* E-mail image in background thread. */
//                        val threadEmail: Thread = object : Thread() {
//                            override fun run() {
//                                sendImageInEmail(imageData, fileName)
//                            }
//                        }
//                        threadEmail.start()
//                    }
//                    .setNegativeButton("Cancel", null)
//            val text =
//                fileNameView.findViewById<View>(R.id.file_name) as EditText
//            text.setText(FILE_NAME_DEFAULT)
//            builder.create().show()
//        }
//    }

    /* *********************************************************************************************
	 * EVENT HANDLERS
	 ******************************************************************************************** */
    /* 
	 * Handle click on "Start capture" button.
	 */
    private val m_btnCaptureStartClickListener =
        View.OnClickListener {
            if (m_bInitializing) return@OnClickListener
            val devIndex = m_cboUsbDevices.selectedItemPosition - 1
            if (devIndex < 0) return@OnClickListener
            if (m_nCurrentCaptureStep != -1) {
                try {
                    val IsActive = iBScanDevice!!.isCaptureActive
                    if (IsActive) {
                        // Capture image manually for active device
                        iBScanDevice!!.captureImageManually()
                        return@OnClickListener
                    }
                } catch (ibse: IBScanException) {
                    _SetStatusBarMessage(
                        "IBScanDevice.takeResultImageManually() returned exception "
                                + ibse.type.toString() + "."
                    )
                }
            }
            if (iBScanDevice == null) {
                m_bInitializing = true
                val thread =
                    _InitializeDeviceThreadCallback(m_nSelectedDevIndex - 1)
                thread.start()
            } else {
                OnMsg_CaptureSeqStart()
            }
            OnMsg_UpdateDisplayResources()
        }

    /*
	 * Handle click on "Stop capture" button. 
	 */
    private val m_btnCaptureStopClickListener =
        View.OnClickListener {
            if (iBScanDevice == null) return@OnClickListener

            // Cancel capturing image for active device.
            try {
                // Cancel capturing image for active device.
                iBScanDevice!!.cancelCaptureImage()
                val tmpInfo = CaptureInfo()
                _SetLEDs(tmpInfo, __LED_COLOR_NONE__, false)
                m_nCurrentCaptureStep = -1
                m_bNeedClearPlaten = false
                m_bBlank = false
                _SetStatusBarMessage("Capture Sequence aborted")
                m_strImageMessage = ""
                _SetImageMessage("")
                OnMsg_UpdateDisplayResources()
            } catch (ibse: IBScanException) {
                _SetStatusBarMessage(
                    "cancel returned exception " + ibse.type.toString() + "."
                )
            }
        }

    /*
	 * Handle long clicks on the image view.
	 */
    private val m_imgPreviewLongClickListener = /*
		 * When the image view is long-clicked, show a popup menu.
		 */
        OnLongClickListener { //			final PopupMenu popup = new PopupMenu(SimpleScanActivity.this, SimpleScanActivity.this.m_txtNFIQ);
//		    popup.setOnMenuItemClickListener(new OnMenuItemClickListener()
//		    {
//		    	/*
//		    	 * Handle click on a menu item.
//		    	 */
//				@Override
//				public boolean onMenuItemClick(final MenuItem item)
//				{
//			        switch (item.getItemId())
//			        {
//			            case R.id.email_image:
//			            	promptForEmail(m_lastResultImage);
//			                return (true);
//			            case R.id.enlarge:
//			            	showEnlargedImage();
//			            	return (true);
//			            default:
//			            	return (false);
//			        }
//				}
//
//		    });
//
//		    final MenuInflater inflater = popup.getMenuInflater();
//		    inflater.inflate(R.menu.scanimage_menu, popup.getMenu());
//		    popup.show();
            true
        }

    /*
	 * Handle click on the spinner that determine the Usb Devices.
	 */
    private val m_cboUsbDevicesItemSelectedListener: OnItemSelectedListener =
        object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View, pos: Int,
                id: Long
            ) {
                OnMsg_cboUsbDevice_Changed()
                m_savedData.usbDevices = pos
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                m_savedData.usbDevices = __INVALID_POS__
            }
        }

    /*
	 * Handle click on the spinner that determine the Fingerprint capture.
	 */
    private val m_captureTypeItemSelectedListener: OnItemSelectedListener =
        object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View, pos: Int,
                id: Long
            ) {
                m_btnCaptureStart.isEnabled = pos != 0
                m_savedData.captureSeq = pos
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                m_savedData.captureSeq = __INVALID_POS__
            }
        }

    /*
	 * Hide the enlarged dialog, if it exists.
	 */
//    private val m_btnCloseEnlargedDialogClickListener =
//        View.OnClickListener {
//            if (m_enlargedDialog != null) {
//                m_enlargedDialog!!.cancel()
//                m_enlargedDialog = null
//            }
//        }

    /*
	 * Handle click on "Enable Spoof" check box
	 */
//    private val m_chkEnableSpoofListener =
//        View.OnClickListener { view ->
//            m_bSpoofEnable = if ((view as CheckBox).isChecked == true) {
//                true
//            } else false
//        }

    /*
	 * Handle click on "Spoof threshold level" combo box / spinner control
	 */
    private val m_cboSpoofThresLevelListener: OnItemSelectedListener =
        object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View,
                i: Int,
                l: Long
            ) {
                m_SpoofThresLevel = SpoofThresLevelAdapter.getItem(i).toString()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    // PUBLIC INTERFACE: IBScanListener METHODS
    // //////////////////////////////////////////////////////////////////////////////////////////////
    override fun scanDeviceAttached(deviceId: Int) {
        showToastOnUiThread("Device $deviceId attached", Toast.LENGTH_SHORT)

        /* 
		 * Check whether we have permission to access this device.  Request permission so it will
		 * appear as an IB scanner. 
		 */
        val hasPermission = m_ibScan!!.hasPermission(deviceId)
        if (!hasPermission) {
            m_ibScan!!.requestPermission(deviceId)
        }
    }

    override fun scanDeviceDetached(deviceId: Int) {
        /*
		 * A device has been detached.  We should also receive a scanDeviceCountChanged() callback,
		 * whereupon we can refresh the display.  If our device has detached while scanning, we 
		 * should receive a deviceCommunicationBreak() callback as well.
		 */
        showToastOnUiThread("Device $deviceId detached", Toast.LENGTH_SHORT)
    }

    override fun scanDevicePermissionGranted(
        deviceId: Int,
        granted: Boolean
    ) {
        if (granted) {
            /*
			 * This device should appear as an IB scanner.  We can wait for the scanDeviceCountChanged()
			 * callback to refresh the display.
			 */
            showToastOnUiThread("Permission granted to device $deviceId", Toast.LENGTH_SHORT)
        } else {
            showToastOnUiThread("Permission denied to device $deviceId", Toast.LENGTH_SHORT)
        }
    }

    override fun scanDeviceCountChanged(deviceCount: Int) {
        OnMsg_UpdateDeviceList(false)
    }

    override fun scanDeviceInitProgress(deviceIndex: Int, progressValue: Int) {
        OnMsg_SetStatusBarMessage("Initializing device...$progressValue%")
    }

    override fun scanDeviceOpenComplete(
        deviceIndex: Int, device: IBScanDevice,
        exception: IBScanException
    ) {
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    // PUBLIC INTERFACE: IBScanDeviceListener METHODS
    // //////////////////////////////////////////////////////////////////////////////////////////////
    override fun deviceCommunicationBroken(device: IBScanDevice) {
        OnMsg_DeviceCommunicationBreak()
    }

    override fun deviceImagePreviewAvailable(
        device: IBScanDevice,
        image: ImageData
    ) {
//		setFrameTime(String.format("%1$.3f ms", image.frameTime*1000));
        OnMsg_DrawImage(device, image)
    }

    override fun deviceFingerCountChanged(
        device: IBScanDevice,
        fingerState: FingerCountState
    ) {
        if (m_nCurrentCaptureStep >= 0) {
            val info =
                m_vecCaptureSeq.elementAt(m_nCurrentCaptureStep)
            if (fingerState == FingerCountState.NON_FINGER) {
                _SetLEDs(info, __LED_COLOR_RED__, true)
            } else {
                _SetLEDs(info, __LED_COLOR_YELLOW__, true)
            }
        }
    }

    override fun deviceFingerQualityChanged(
        device: IBScanDevice,
        fingerQualities: Array<FingerQualityState>
    ) {
        for (i in fingerQualities.indices) {
            m_FingerQuality[i] = fingerQualities[i]
        }

//		OnMsg_DrawFingerQuality();
    }

    override fun deviceAcquisitionBegun(
        device: IBScanDevice,
        imageType: ImageType
    ) {
        if (imageType == ImageType.ROLL_SINGLE_FINGER) {
            OnMsg_Beep(__BEEP_OK__)
            m_strImageMessage = "When done remove finger from sensor"
            _SetImageMessage(m_strImageMessage)
            _SetStatusBarMessage(m_strImageMessage)
        }
    }

    override fun deviceAcquisitionCompleted(
        device: IBScanDevice,
        imageType: ImageType
    ) {
        if (imageType == ImageType.ROLL_SINGLE_FINGER) {
            OnMsg_Beep(__BEEP_OK__)
        } else {
            OnMsg_Beep(__BEEP_SUCCESS__)
            _SetImageMessage("Remove fingers from sensor")
            _SetStatusBarMessage("Acquisition completed, postprocessing..")
        }
    }

    override fun deviceImageResultAvailable(
        device: IBScanDevice, image: ImageData,
        imageType: ImageType, splitImageArray: Array<ImageData>
    ) {
        /* TODO: ALTERNATIVELY, USE RESULTS IN THIS FUNCTION */
    }

    override fun deviceImageResultExtendedAvailable(
        device: IBScanDevice,
        imageStatus: IBScanException?,
        image: ImageData,
        imageType: ImageType,
        detectedFingerCount: Int,
        segmentImageArray: Array<ImageData?>,
        segmentPositionArray: Array<SegmentPosition>
    ) {
//		setFrameTime(String.format("%1$.3f ms", image.frameTime*1000));
        m_savedData.imagePreviewImageClickable = true
        m_imgPreview!!.isLongClickable = true
        m_lastResultImage = image
        m_lastSegmentImages = segmentImageArray

        // imageStatus value is greater than "STATUS_OK", Image acquisition successful.
        if (imageStatus == null /*STATUS_OK*/ ||
            imageStatus.type.compareTo(IBScanException.Type.INVALID_PARAM_VALUE) > 0
        ) {
            if (imageType == ImageType.ROLL_SINGLE_FINGER) {
                OnMsg_Beep(__BEEP_SUCCESS__)
            }
        }
        if (m_bNeedClearPlaten) {
            m_bNeedClearPlaten = false
            //			OnMsg_DrawFingerQuality();
        }

        // imageStatus value is greater than "STATUS_OK", Image acquisition successful.
        if (imageStatus == null /*STATUS_OK*/ ||
            imageStatus.type.compareTo(IBScanException.Type.INVALID_PARAM_VALUE) > 0
        ) {
            // Image acquisition successful
            val info =
                m_vecCaptureSeq.elementAt(m_nCurrentCaptureStep)
            _SetLEDs(info, __LED_COLOR_GREEN__, false)

            // SAVE IMAGE
/*			if (m_chkSaveImages.isSelected())
			{
				// Show chooser for output image.
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(imageFilter);
				int returnVal = chooser.showSaveDialog(IBScanUltimate_Sample.this);

				if (returnVal == JFileChooser.APPROVE_OPTION)
				{						
					_SetStatusBarMessage("Saving image...");
					m_ImgSaveFolderName = chooser.getCurrentDirectory().toString() + File.separator + chooser.getSelectedFile().getName();
					_SaveBitmapImage(image, info.fingerName);
					_SaveWsqImage(image, info.fingerName);
					_SavePngImage(image, info.fingerName);
					_SaveJP2Image(image, info.fingerName);

					//save segmented fingers
					for (int i = 0; i < detectedFingerCount; i++)
				{
						String segmentName = info.fingerName + "_Segment_" + String.valueOf(i);
						_SaveBitmapImage(segmentImageArray[i], segmentName);
						_SaveWsqImage(segmentImageArray[i], segmentName);
						_SavePngImage(segmentImageArray[i], segmentName);
						_SaveJP2Image(segmentImageArray[i], segmentName);
				}
			}
			}
*/
//			if (m_chkDrawSegmentImage.isSelected())
            run {
                m_nSegmentImageArrayCount = detectedFingerCount
                m_SegmentPositionArray = segmentPositionArray
            }

            // NFIQ
//			if (m_chkNFIQScore.isSelected())
            run {
                val nfiq_score = byteArrayOf(0, 0, 0, 0)
                try {
                    var i = 0
                    var segment_pos = 0
                    while (i < 4) {
                        if (m_FingerQuality[i].ordinal != FingerQualityState.FINGER_NOT_PRESENT.ordinal) {
                            nfiq_score[i] = iBScanDevice!!.calculateNfiqScore(
                                segmentImageArray[segment_pos++]
                            ).toByte()
                        }
                        i++
                    }
                } catch (ibse: IBScanException) {
                    ibse.printStackTrace()
                }
                OnMsg_SetTxtNFIQScore(
                    "" + nfiq_score[0] + "-" + nfiq_score[1] + "-" + nfiq_score[2] + "-" + nfiq_score[3]
                )
            }
            if (imageStatus == null /*STATUS_OK*/) {
                m_strImageMessage = "Acquisition completed successfully"
                _SetImageMessage(m_strImageMessage)
                _SetStatusBarMessage(m_strImageMessage)

                activity?.runOnUiThread {
                    viewModel.getVoter("867-49-6671")
                }
            } else {
                // > IBSU_STATUS_OK
                m_strImageMessage =
                    "Acquisition Warning (Warning code = " + imageStatus.type.toString() + ")"
                _SetImageMessage(m_strImageMessage)
                _SetStatusBarMessage(m_strImageMessage)
                OnMsg_DrawImage(device, image)
                OnMsg_AskRecapture(imageStatus)
                return
            }
        } else {
            // < IBSU_STATUS_OK
            m_strImageMessage =
                "Acquisition failed (Error code = " + imageStatus.type.toString() + ")"
            _SetImageMessage(m_strImageMessage)
            _SetStatusBarMessage(m_strImageMessage)

            // Stop all of acquisition
            m_nCurrentCaptureStep = m_vecCaptureSeq.size
        }
        OnMsg_DrawImage(device, image)
        OnMsg_CaptureSeqNext()
    }

    override fun devicePlatenStateChanged(
        device: IBScanDevice,
        platenState: PlatenState
    ) {
        m_bNeedClearPlaten = platenState == PlatenState.HAS_FINGERS
        if (platenState == PlatenState.HAS_FINGERS) {
            m_strImageMessage = "Please remove your fingers on the platen first!"
            _SetImageMessage(m_strImageMessage)
            _SetStatusBarMessage(m_strImageMessage)
        } else {
            if (m_nCurrentCaptureStep >= 0) {
                val info =
                    m_vecCaptureSeq.elementAt(m_nCurrentCaptureStep)

                // Display message for image acuisition again
                val strMessage = info.PreCaptureMessage
                _SetStatusBarMessage(strMessage)
                //				if (!m_chkAutoCapture.isSelected())
//					strMessage += "\r\nPress button 'Take Result Image' when image is good!";
                _SetImageMessage(strMessage)
                m_strImageMessage = strMessage
            }
        }

//		OnMsg_DrawFingerQuality();
    }

    override fun deviceWarningReceived(
        device: IBScanDevice,
        warning: IBScanException
    ) {
        _SetStatusBarMessage("Warning received " + warning.type.toString())
    }

    override fun devicePressedKeyButtons(
        device: IBScanDevice,
        pressedKeyButtons: Int
    ) {
        _SetStatusBarMessage("PressedKeyButtons $pressedKeyButtons")
        val selectedDev = m_cboUsbDevices.selectedItemPosition > 0
        val idle = m_bInitializing && m_nCurrentCaptureStep == -1
        val active = m_bInitializing && m_nCurrentCaptureStep != -1
        try {
            if (pressedKeyButtons == __LEFT_KEY_BUTTON__) {
                if (selectedDev && idle) {
                    println("Capture Start")
                    device.setBeeper(
                        BeepPattern.BEEP_PATTERN_GENERIC,
                        2 /*Sol*/,
                        4 /*100ms = 4*25ms*/,
                        0,
                        0
                    )
                    m_btnCaptureStart.performClick()
                }
            } else if (pressedKeyButtons == __RIGHT_KEY_BUTTON__) {
                if (active) {
                    println("Capture Stop")
                    device.setBeeper(
                        BeepPattern.BEEP_PATTERN_GENERIC,
                        2 /*Sol*/,
                        4 /*100ms = 4*25ms*/,
                        0,
                        0
                    )
                    m_btnCaptureStop.performClick()
                }
            }
        } catch (e: IBScanException) {
            e.printStackTrace()
        }
    }

    companion object {
        /* *********************************************************************************************
	 * PRIVATE CONSTANTS
	 ******************************************************************************************** */
        /* The tag used for Android log messages from this app. */
        private const val TAG = "Simple Scan"
        private const val __INVALID_POS__ = -1

        /* The default value of the status TextView. */
        private const val __NFIQ_DEFAULT__ = "0-0-0-0"

        /* The default value of the frame time TextView. */
        private const val __NA_DEFAULT__ = "n/a"

        /* The default file name for images and templates for e-mail. */
        private const val FILE_NAME_DEFAULT = "output"

        /* The number of finger qualities set in the preview image. */
        private const val FINGER_QUALITIES_COUNT = 4

        /* The background color of the preview image ImageView. */
        private const val PREVIEW_IMAGE_BACKGROUND = Color.LTGRAY

        /* The background color of a finger quality TextView when the finger is not present. */
        private const val FINGER_QUALITY_NOT_PRESENT_COLOR = Color.LTGRAY

        /* The background color of a finger quality TextView when the finger is good quality. */
        private const val FINGER_QUALITY_GOOD_COLOR = Color.GREEN

        /* The background color of a finger quality TextView when the finger is fair quality. */
        private const val FINGER_QUALITY_FAIR_COLOR = Color.YELLOW

        /* The background color of a finger quality TextView when the finger is poor quality. */
        private const val FINGER_QUALITY_POOR_COLOR = Color.RED

        /* The number of finger segments set in the result image. */
        private const val FINGER_SEGMENT_COUNT = 4

        /*
	 * Exit application.
	 */
        private fun exitApp(ac: Activity) {
            ac.moveTaskToBack(true)
            ac.finish()
            Process.killProcess(Process.myPid())
        }
    }
}
