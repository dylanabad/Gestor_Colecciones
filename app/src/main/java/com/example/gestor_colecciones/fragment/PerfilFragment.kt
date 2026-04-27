package com.example.gestor_colecciones.fragment

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.databinding.FragmentPerfilBinding
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.network.UploadUtils
import com.example.gestor_colecciones.repository.PerfilRepository
import com.example.gestor_colecciones.util.ImageUtils
import com.example.gestor_colecciones.viewmodel.PerfilState
import com.example.gestor_colecciones.viewmodel.PerfilViewModel
import com.example.gestor_colecciones.viewmodel.PerfilViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.File

/**
 * Pantalla de perfil del coleccionista.
 *
 * Permite al usuario visualizar y editar su información personal (nombre, biografía)
 * y su imagen de avatar. También muestra estadísticas agregadas de su actividad,
 * como el número total de colecciones, ítems y logros obtenidos.
 *
 * Gestiona permisos de cámara y almacenamiento para la actualización de la foto de perfil.
 */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PerfilViewModel

    private var selectedAvatarUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var removeAvatar: Boolean = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAvatarUri = it
            removeAvatar = false
            binding.ivAvatar.setImageURI(it)
        }
    }

    private val takeImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let {
                selectedAvatarUri = it
                removeAvatar = false
                binding.ivAvatar.setImageURI(it)
                finalizePendingImage(it)
            }
        } else {
            takeImagePreviewLauncher.launch(null)
        }
    }

    private val takeImagePreviewLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToGallery(bitmap, "avatar")
            if (uri != null) {
                selectedAvatarUri = uri
                removeAvatar = false
                binding.ivAvatar.setImageURI(uri)
            } else {
                showSnackbar("No se pudo guardar la foto")
            }
        } else {
            showSnackbar("No se pudo capturar la foto")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val cameraGranted = granted[Manifest.permission.CAMERA] == true
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            granted[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        } else true

        if (cameraGranted && storageGranted) {
            openCameraForAvatar()
        } else {
            showSnackbar("Permiso de c\u00e1mara denegado")
        }
    }

    /**
     * Infla el diseño del fragmento utilizando ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Inicializa el ViewModel, configura los listeners de la UI y comienza la observación
     * de los flujos de datos (perfil, estadísticas y estados).
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = com.example.gestor_colecciones.database.DatabaseProvider.getDatabase(requireContext())
        val repo = PerfilRepository(
            ApiProvider.getApi(requireContext()),
            db.coleccionDao(),
            db.itemDao(),
            db.logroDao()
        )
        viewModel = ViewModelProvider(this, PerfilViewModelFactory(repo))[PerfilViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCambiarFoto.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnQuitarFoto.setOnClickListener {
            removeAvatar = true
            selectedAvatarUri = null
            binding.ivAvatar.setImageResource(R.drawable.ic_person_24)
        }

        binding.btnGuardar.setOnClickListener {
            val displayName = binding.etDisplayName.text?.toString()?.trim()
            val bio = binding.etBio.text?.toString()?.trim()

            val avatarPart: MultipartBody.Part? = selectedAvatarUri?.let { uri ->
                UploadUtils.createImagePart(requireContext(), uri)
            }

            viewModel.guardarCambios(
                displayName = displayName,
                bio = bio,
                avatarPart = avatarPart,
                removeAvatar = removeAvatar
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.perfil.collectLatest { perfil ->
                if (perfil == null) return@collectLatest

                val nombreVisible = (perfil.displayName?.takeIf { it.isNotBlank() } ?: perfil.username)
                binding.tvDisplayName.text = nombreVisible
                binding.tvUsernameEmail.text = "@${perfil.username} \u2022 ${perfil.email}"

                if (binding.etDisplayName.text.isNullOrBlank()) {
                    binding.etDisplayName.setText(perfil.displayName.orEmpty())
                }
                if (binding.etBio.text.isNullOrBlank()) {
                    binding.etBio.setText(perfil.bio.orEmpty())
                }

                if (!removeAvatar && selectedAvatarUri == null) {
                    val model = ImageUtils.toGlideModel(perfil.avatarPath)
                    if (model != null) {
                        Glide.with(this@PerfilFragment)
                            .load(model)
                            .placeholder(R.drawable.ic_person_24)
                            .error(R.drawable.ic_person_24)
                            .into(binding.ivAvatar)
                    } else {
                        binding.ivAvatar.setImageResource(R.drawable.ic_person_24)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                if (stats == null) return@collectLatest
                binding.tvStatCollections.text = stats.collections.toString()
                binding.tvStatItems.text = stats.items.toString()
                binding.tvStatLogros.text = stats.logros.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is PerfilState.Loading -> setLoading(true)
                    is PerfilState.Error -> {
                        setLoading(false)
                        showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    is PerfilState.Saved -> {
                        setLoading(false)
                        removeAvatar = false
                        selectedAvatarUri = null
                        showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    else -> setLoading(false)
                }
            }
        }

        viewModel.cargarPerfil()
    }

    /**
     * Controla el estado de carga de la interfaz, habilitando o deshabilitando botones
     * y mostrando el indicador de progreso.
     */
    private fun setLoading(loading: Boolean) {
        binding.btnGuardar.isEnabled = !loading
        binding.btnCambiarFoto.isEnabled = !loading
        binding.btnQuitarFoto.isEnabled = !loading
        binding.layoutLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Muestra un diálogo para que el usuario elija entre tomar una foto con la cámara
     * o seleccionar una imagen de la galería.
     */
    private fun showImageSourceDialog() {
        val options = arrayOf(getString(R.string.galeria), getString(R.string.camara))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.seleccionar_avatar))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> {
                        val cameraGranted = ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            ContextCompat.checkSelfPermission(
                                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true

                        if (cameraGranted && storageGranted) {
                            openCameraForAvatar()
                        } else {
                            val perms = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else arrayOf(Manifest.permission.CAMERA)
                            cameraPermissionLauncher.launch(perms)
                        }
                    }
                }
            }
            .show()
    }

    /**
     * Prepara el entorno para abrir la cámara, creando un URI temporal en la galería
     * para almacenar la fotografía capturada.
     */
    private fun openCameraForAvatar() {
        val uri = createGalleryImageUri("avatar")
        cameraImageUri = uri
        takeImageLauncher.launch(uri)
    }

    /**
     * Crea una entrada en [MediaStore] para una nueva imagen, permitiendo que la cámara
     * guarde el archivo directamente en la galería del sistema.
     *
     * @param prefix Prefijo para el nombre del archivo generado.
     * @return URI de la ubicación donde se guardará la imagen.
     */
    private fun createGalleryImageUri(prefix: String): Uri {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val destDir = File(picturesDir, "GestorColecciones").also { it.mkdirs() }
                val file = File(destDir, fileName)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("No se pudo crear la imagen en la galeria")
    }

    /**
     * Notifica al sistema que una imagen que estaba en estado "pendiente" ya ha sido
     * escrita completamente en el almacenamiento.
     */
    private fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            try {
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Guarda un objeto [Bitmap] en la galería del dispositivo y devuelve su [Uri].
     * Útil para capturas rápidas o previsualizaciones de cámara.
     */
    private fun saveBitmapToGallery(bitmap: Bitmap, prefix: String): Uri? {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val destDir = File(picturesDir, "GestorColecciones").also { it.mkdirs() }
                val file = File(destDir, fileName)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        finalizePendingImage(uri)
        return uri
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

