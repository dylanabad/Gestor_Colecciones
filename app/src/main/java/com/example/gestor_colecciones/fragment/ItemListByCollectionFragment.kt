package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ItemListByCollectionFragment : Fragment() {

    private var collectionId: Int = 0
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recoger el ID de la colección pasado en argumentos
        collectionId = arguments?.getInt(ARG_COLLECTION_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar ViewModel
        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = ItemRepository(db.itemDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(repo)
        )[ItemViewModel::class.java]

        // Configurar RecyclerView
        adapter = ItemAdapter(emptyList())
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        // Observar los items de la colección
        lifecycleScope.launch {
            viewModel.getItemsByCollection(collectionId).collectLatest { items ->
                adapter.updateList(items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"

        fun newInstance(collectionId: Int): ItemListByCollectionFragment {
            val fragment = ItemListByCollectionFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_COLLECTION_ID, collectionId)
            }
            return fragment
        }
    }
}