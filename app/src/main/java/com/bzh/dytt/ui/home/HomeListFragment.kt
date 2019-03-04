package com.bzh.dytt.ui.home

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bzh.dytt.AppExecutors
import com.bzh.dytt.R
import com.bzh.dytt.SingleActivity
import com.bzh.dytt.base.BaseFragment
import com.bzh.dytt.databinding.HomeListPageBinding
import com.bzh.dytt.databinding.ItemHomeChildBinding
import com.bzh.dytt.databinding.ItemLoadMoreBinding
import com.bzh.dytt.ui.detail.InnerDialogFragment
import com.bzh.dytt.util.ThunderHelper
import com.bzh.dytt.util.autoCleared
import com.bzh.dytt.vo.MovieDetail
import com.yarolegovich.lovelydialog.LovelyChoiceDialog
import javax.inject.Inject


class HomeListFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appExecutors: AppExecutors

    private var refreshListener: androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener = androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {
        viewModel.doRefreshFirstPage()
    }

    private var onScrollListener: androidx.recyclerview.widget.RecyclerView.OnScrollListener = object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (recyclerView.layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
                val linearLayoutManager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                val itemCount = linearLayoutManager.itemCount
                val completelyVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                if (!viewModel.isLoadMore && completelyVisibleItemPosition == (itemCount - 1)) {
                    viewModel.doLoadMorePage()
                }
            }
        }
    }

    private var listObserver: Observer<List<MovieDetail>> = Observer { result ->
        adapter.submitList(result)
    }

    private val refreshObserver: Observer<Boolean> = Observer {
        binding.swipeRefreshLayout.isRefreshing = (it == true)
    }

    private lateinit var viewModel: HomeListViewModel

    private lateinit var adapter: HomeListAdapter

    private lateinit var linearLayoutManager: androidx.recyclerview.widget.LinearLayoutManager

    private var binding by autoCleared<HomeListPageBinding>()

    override fun doCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.home_list_page, container, false)

        viewModel = viewModelFactory.create(HomeListViewModel::class.java)
        lifecycle.addObserver(viewModel)

        binding.viewModel = viewModel

        return binding.root
    }

    override fun doViewCreated(view: View, savedInstanceState: Bundle?) {
        super.doViewCreated(view, savedInstanceState)

        val movieType = arguments?.getSerializable(MOVIE_TYPE)

        viewModel.moveTypeLiveData.value = movieType as HomeViewModel.HomeMovieType
        binding.swipeRefreshLayout.setOnRefreshListener(refreshListener)

        binding.recyclerView.addOnScrollListener(onScrollListener)

        linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager

        adapter = HomeListAdapter(activity, viewModel, appExecutors)
        binding.recyclerView.adapter = adapter

        viewModel.movieListLiveData.observe(this, listObserver)

        viewModel.refreshLiveData.observe(this, refreshObserver)
    }

    companion object {

        private const val TAG = "HomeListFragment"

        private const val MOVIE_TYPE = "MOVIE_TYPE"

        fun newInstance(movieType: HomeViewModel.HomeMovieType): HomeListFragment {
            val args = Bundle()
            args.putSerializable(MOVIE_TYPE, movieType)
            val fragment = HomeListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}


class HomeListAdapter(val activity: androidx.fragment.app.FragmentActivity?, val viewModel: HomeListViewModel, appExecutors: AppExecutors) : ListAdapter<MovieDetail, androidx.recyclerview.widget.RecyclerView.ViewHolder>(AsyncDifferConfig
        .Builder<MovieDetail>(object : DiffUtil.ItemCallback<MovieDetail>() {
            override fun areItemsTheSame(oldItem: MovieDetail, newItem: MovieDetail): Boolean {
                return oldItem.id == newItem.id && oldItem.categoryId == newItem.categoryId
            }

            override fun areContentsTheSame(oldItem: MovieDetail, newItem: MovieDetail): Boolean {
                return oldItem.name == newItem.name && oldItem.homePicUrl == newItem.homePicUrl
            }
        })
        .setBackgroundThreadExecutor(appExecutors.diskIO())
        .build()
) {


    override fun getItem(position: Int): MovieDetail {
        return if (position >= super.getItemCount()) {
            MovieDetail.createEmptyMovieDetail()
        } else {
            super.getItem(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position >= super.getItemCount()) {
            ITEM_LOAD_MORE_TYPE
        } else {
            super.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int {
        return if (super.getItemCount() == 0) {
            super.getItemCount()
        } else {
            super.getItemCount() + ITEM_LOAD_MORE_COUNT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        if (viewType == ITEM_LOAD_MORE_TYPE) {
            return LoadMoreItemHolder(
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                            R.layout.item_load_more,
                            parent, false
                    )
            )
        } else {
            return MovieItemHolder(activity,
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.item_home_child,
                            parent,
                            false
                    )
            )
        }
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == ITEM_LOAD_MORE_TYPE) {

        } else {
            getItem(position).let { movieDetail ->
                Log.d(TAG, "onBindViewHolder() called with: movieDetail = [${movieDetail.id} ${movieDetail.name}]")
                with(holder as MovieItemHolder) {
                    itemView.tag = movieDetail
                    reset()
                    bind(movieDetail)
                }
            }
        }
    }

    override fun onViewAttachedToWindow(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder.adapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
            getItem(holder.adapterPosition).let {
                viewModel.doUpdateMovieDetail(it)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder.adapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
            getItem(holder.adapterPosition).let {
                viewModel.doRemoveUpdateMovieDetail(it)
            }
        }
    }

    companion object {
        const val TAG = "HomeListAdapter"
        const val ITEM_LOAD_MORE_COUNT = 1
        const val ITEM_LOAD_MORE_TYPE = 1000
    }

}

class LoadMoreItemHolder(val binding: ItemLoadMoreBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)


class MovieItemHolder(val activity: androidx.fragment.app.FragmentActivity?, val binding: ItemHomeChildBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    val thunderHelper: ThunderHelper = ThunderHelper()

    fun bind(movieDetail: MovieDetail) {
        with(binding) {
            viewModel = ItemChildViewModel(movieDetail)
            if (activity != null) {
                viewModel?.clickObserver?.observe(activity, Observer {
                    SingleActivity.startDetailPage(activity, binding.videoCover, binding.videoCover.transitionName, movieDetail)
                })
                if (movieDetail.isPrefect) {
                    binding.root.setOnLongClickListener {
                        val url = movieDetail.downloadUrl?.split(";".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray().orEmpty()
                        LovelyChoiceDialog(activity)
                                .setTopColorRes(R.color.colorPrimary)
                                .setIcon(R.drawable.ic_download_white)
                                .setItemsMultiChoice(url) { _, items ->
                                    if (!thunderHelper.checkIsInstall(activity)) {
                                        val dialogFragment = InnerDialogFragment()
                                        dialogFragment.show(activity.supportFragmentManager, "InnerDialog")
                                    } else {
                                        for (item in items) {
                                            thunderHelper.startThunder(activity, item)
                                        }
                                    }

                                }
                                .setConfirmButtonText(R.string.ok)
                                .show()
                        true
                    }
                }
            }
            executePendingBindings()
        }
    }

    fun reset() {
        binding.videoCover.setImageResource(R.drawable.default_video)
    }

    companion object {
        const val TAG = "MovieItemHolder"
    }
}