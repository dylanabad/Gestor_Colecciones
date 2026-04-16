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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = PerfilRepository(ApiProvider.getApi(requireContext()))
        viewModel = ViewModelProvider(this, PerfilViewModelFactory(repo))[PerfilViewModel::class.java]

        binding.btnBack.setOnClickListener {
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

                binding.etDisplayName.setText(perfil.displayName.orEmpty())
                binding.etBio.setText(perfil.bio.orEmpty())

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

    private fun setLoading(loading: Boolean) {
        binding.btnGuardar.isEnabled = !loading
        binding.btnCambiarFoto.isEnabled = !loading
        binding.btnQuitarFoto.isEnabled = !loading
        binding.tvLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Galeria", "Camara")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar avatar")
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

    private fun openCameraForAvatar() {
        val uri = createGalleryImageUri("avatar")
        cameraImageUri = uri
        takeImageLauncher.launch(uri)
    }

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

    private fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, values, null, null)
        }
    }

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

