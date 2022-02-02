package ru.netology.nmedia.adapter

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.PopupMenu
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback()) {

    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        val anim = holder.itemView.getTag(R.id.like + holder.absoluteAdapterPosition)
        //завершаем анимацию при выходе за пределы экрана
        if( anim != null){
            println("not empty")
            (anim as ObjectAnimator).end()
        } else println (" tag not found")
    }

    override fun onViewAttachedToWindow(holder: PostViewHolder) {
        val anim = holder.itemView.getTag(R.id.like + holder.absoluteAdapterPosition)
        //ещё шанс посмотреть остановленную анимацию при возврате на экран
        if(anim != null ){
            println("not empty")
            (anim as ObjectAnimator).start()
            //удаляем анимацию при её остановки
            anim.doOnEnd {
                holder.itemView.setTag(R.id.like + holder.absoluteAdapterPosition, null)
            }
        } else println (" tag not found")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)

    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
       if (payloads.isEmpty()){
           onBindViewHolder(holder, position)
       } else {
           payloads.forEach {
               if(it is Payload ){
                   holder.bind(it)
               }
           }
       }
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {


    fun bind(payload: Payload){
        binding.apply {
            payload.likedByMe?.let { likeByMe ->
                like.setImageResource(
                    if(likeByMe) R.drawable.ic_liked_24 else R.drawable.ic_like_24
                )

                if (likeByMe) {
                    ObjectAnimator.ofPropertyValuesHolder(
                        like,
                        PropertyValuesHolder.ofFloat(View.SCALE_X,1.0F,2F,1.0F),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y,1.0F,2F,1.0F)
                    )
                } else {
                    ObjectAnimator.ofFloat(
                        like,
                        View.ROTATION,
                        0F,
                        360F
                    )
                }.apply {
                    duration = 1000
                    repeatCount = 15
                    interpolator = BounceInterpolator()
                    //если анимация есть, заканчиваем ее
                    root.getTag(R.id.like + absoluteAdapterPosition)?.let {
                        (it as ObjectAnimator).end()
                    }
                    //суём аниматор в тэг с ключом, специфичным для данного поста
                    root.setTag(R.id.like + absoluteAdapterPosition,this)
                }.start()
            }

            payload.content?.let {
                content.text = it
            }
        }
    }

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = " ${post.published}"
            content.text = post.content
            like.setImageResource(
                if (post.likedByMe) R.drawable.ic_liked_24 else R.drawable.ic_like_24
            )

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }
        }
    }
}

data class Payload(
    val likedByMe: Boolean? = null,
    val content: String? = null,
)

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Post, newItem: Post): Any =
        Payload(
            likedByMe = newItem.likedByMe.takeIf { it!= oldItem.likedByMe },
            content = newItem.content.takeIf { it != oldItem.content }
        )

}
