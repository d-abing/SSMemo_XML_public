import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.databinding.RecyclerClassifyMemoBinding
import com.aube.ssgmemo.etc.MyApplication

class ViewPagerAdapter(private val context: Context) :
    ListAdapter<Memo, ViewPagerAdapter.Holder>(diffUtil) {

    private val memofont = MyApplication.prefs.getString("memofont", "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding =
            RecyclerClassifyMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val memo = getItem(position)
        holder.setMemo(memo)
    }

    inner class Holder(val binding: RecyclerClassifyMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setMemo(memo: Memo) {
            binding.memoTitle.text = memo.title

            // 폰트 설정
            if (!memofont.isNullOrEmpty()) {
                val typeface = Typeface.createFromAsset(context.assets, "font/$memofont.ttf")
                binding.memoTitle.typeface = typeface
                binding.memoContent.typeface = typeface
            }

            val contentAtt = memo.contentAttribute.split(",")
            val contentGravity = contentAtt[0].toInt()
            binding.memoContent.text = Html.fromHtml(memo.content)
            binding.memoContent.movementMethod = ScrollingMovementMethod()
            binding.memoContent.gravity = contentGravity
        }
    }


    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Memo>() {
            override fun areItemsTheSame(oldItem: Memo, newItem: Memo): Boolean {
                return oldItem.idx == newItem.idx
            }

            override fun areContentsTheSame(oldItem: Memo, newItem: Memo): Boolean {
                return oldItem == newItem
            }
        }
    }
}