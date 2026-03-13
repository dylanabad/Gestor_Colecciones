package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
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

class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        adapter = ItemAdapter(emptyList())
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        // ViewModel
        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = ItemRepository(db.itemDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(repo)
        )[ItemViewModel::class.java]

        // Observar lista de ítems
        lifecycleScope.launch {
            viewModel.items.collectLatest { items ->
                adapter.updateList(items)
            }
        }

        // Configurar búsqueda usando SearchView
        binding.searchItems.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Cuando el usuario presiona "Enter"
                query?.let { search(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Cada vez que cambia el texto
                newText?.let { search(it) }
                return true
            }

            private fun search(query: String) {
                lifecycleScope.launch {
                    viewModel.searchItems(query).collectLatest { items ->
                        adapter.updateList(items)
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}