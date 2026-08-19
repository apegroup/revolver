import SwiftUI
import shared // Replace with your KMP framework module name

/// Search screen: query bar, paginated results list, and load-more footer —
/// driven by `SearchObservableViewModel`.
struct SearchView: View {

    @ObservedObject var viewModel: SearchObservableViewModel
    @State private var query = ""
    @State private var scrollProxy: ScrollViewProxy? = nil

    var body: some View {
        VStack(spacing: 0) {
            searchBar

            Divider()

            content
        }
        // Animate to item 0 whenever a ScrollToTop effect fires.
        .onChange(of: viewModel.scrollTrigger) { _ in
            withAnimation { scrollProxy?.scrollTo(0, anchor: .top) }
        }
    }

    // MARK: - Sub-views

    private var searchBar: some View {
        HStack {
            TextField("Search…", text: $query)
                .textFieldStyle(.roundedBorder)
                .submitLabel(.search)
                .onSubmit { viewModel.search(query: query) }

            Button {
                viewModel.search(query: query)
            } label: {
                Image(systemName: "magnifyingglass")
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case is SearchState.Idle:
            idleHint

        case is SearchState.InitialLoading:
            ProgressView("Searching…")
                .frame(maxWidth: .infinity, maxHeight: .infinity)

        case let results as SearchState.Results:
            ResultsList(
                state: results,
                onLoadMore: viewModel.loadMore,
                scrollProxy: $scrollProxy
            )

        case let error as SearchState.Error:
            errorView(message: error.message)

        default:
            EmptyView()
        }
    }

    private var idleHint: some View {
        Text("Enter a query above to search")
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass.circle")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text(message)
                .foregroundStyle(.red)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ResultsList: View {
    let state: SearchState.Results
    let onLoadMore: () -> Void
    @Binding var scrollProxy: ScrollViewProxy?

    var body: some View {
        ScrollViewReader { proxy in
            List {
                ForEach(Array(state.items.enumerated()), id: \.offset) { idx, item in
                    Text(item)
                        .id(idx == 0 ? 0 : nil) // anchor for scrollTo(0)
                }

                // Footer: loading indicator, load-more button, or end-of-results label.
                if state.isNextPageLoading {
                    HStack { Spacer(); ProgressView(); Spacer() }
                        .listRowSeparator(.hidden)
                } else if state.hasMore {
                    Button("Load more", action: onLoadMore)
                        .frame(maxWidth: .infinity)
                        .buttonStyle(.bordered)
                        .listRowSeparator(.hidden)
                } else {
                    Text("No more results")
                        .foregroundStyle(.secondary)
                        .font(.footnote)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.plain)
            .onAppear { scrollProxy = proxy }
        }
    }
}
