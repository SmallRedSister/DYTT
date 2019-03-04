package com.bzh.dytt.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bzh.dytt.ui.detail.DetailViewModel
import com.bzh.dytt.ui.home.HomeListViewModel
import com.bzh.dytt.ui.home.HomeViewModel
import com.bzh.dytt.ui.search.SearchViewModel
import com.bzh.dytt.util.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap


@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(DetailViewModel::class)
    abstract fun bindDetailViewModel(viewModel: DetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(homeViewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeListViewModel::class)
    abstract fun bindHomeListViewModel(homeListViewModel: HomeListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModel::class)
    abstract fun bindSearchViewModel(searchViewModel: SearchViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

}
