/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

const Backbone = require('backbone')
const $ = require('jquery')
const _ = require('underscore')
const properties = require('../properties.js')
const QueryResponse = require('./QueryResponse.js')
// const ResultSort = require('./ResultSort.js')
const Sources = require('../../component/singletons/sources-instance.js')
const Common = require('../Common.js')
const CacheSourceSelector = require('../CacheSourceSelector.js')
const announcement = require('../../component/announcement/index.jsx')
const CQLUtils = require('../CQLUtils.js')
const user = require('../../component/singletons/user-instance.js')
const _merge = require('lodash/merge')
require('backbone-associations')
import PartialAssociatedModel from '../../js/extensions/backbone.partialAssociatedModel'
const plugin = require('plugins/query')
import React from 'react'
import { readableColor } from 'polished'
import { LazyQueryResults } from './LazyQueryResult/LazyQueryResults'
const Query = {}

function limitToDeleted(cqlString) {
  return CQLUtils.transformFilterToCQL({
    type: 'AND',
    filters: [
      CQLUtils.transformCQLToFilter(cqlString),
      {
        property: '"metacard-tags"',
        type: 'ILIKE',
        value: 'deleted',
      },
    ],
  })
}

function limitToHistoric(cqlString) {
  return CQLUtils.transformFilterToCQL({
    type: 'AND',
    filters: [
      CQLUtils.transformCQLToFilter(cqlString),
      {
        property: '"metacard-tags"',
        type: 'ILIKE',
        value: 'revision',
      },
    ],
  })
}

const handleTieredSearchLocalFinish = function(ids) {
  const results = this.get('result')
    .get('results')
    .toJSON()

  const status = this.get('result')
    .get('status')
    .toJSON()

  const resultIds = results.map(result => result.metacard.id)
  const missingResult = ids.some(id => !resultIds.includes(id))
  if (!missingResult) {
    return
  }
  this.set('federation', 'enterprise')
  this.startSearch({ results, status })
}

const reducer = (state = [{}], action) => {
  switch (action.type) {
    case 'CLEAR_PAGES':
      return [{}]
    case 'NEXT_PAGE':
      return state.concat({})
    case 'PREVIOUS_PAGE':
      return state.slice(0, -1)
    case 'UPDATE_RESULTS':
      console.log('UPDATE_RESULTS:: ', action.payload)
      const srcs = action.payload.status
        .filter(status => status.id !== 'cache')
        .map(({ id, count }) => ({ id, count }))
        .reduce((counts, status) => {
          const { id, count } = status
          if (count !== 0) {
            counts[id] = count
          }
          return counts
        }, {})

      return state.slice(0, -1).concat(srcs)
    default:
      return state
  }
}

const currentIndexForSource = state => {
  return state.reduce((counts, page) => {
    return Object.keys(page).reduce((counts, src) => {
      const resultCount = page[src]

      if (counts[src] === undefined) {
        counts[src] = 1
      }
      counts[src] += resultCount

      return counts
    }, counts)
  }, {})
}

const serverPageIndex = state => state.length

const previousPage = () => ({ type: 'PREVIOUS_PAGE' })
const nextPage = () => ({ type: 'NEXT_PAGE' })
const clearPages = () => ({ type: 'CLEAR_PAGES' })
const updateResults = payload => ({ type: 'UPDATE_RESULTS', payload })

Query.Model = PartialAssociatedModel.extend({
  relations: [
    {
      type: Backbone.One,
      key: 'result',
      relatedModel: QueryResponse,
      isTransient: true,
    },
  ],
  dispatch(action) {
    this.state = reducer(this.state, action)
  },
  set(data, ...args) {
    if (
      typeof data === 'object' &&
      data.filterTree !== undefined &&
      typeof data.filterTree === 'string'
    ) {
      // for backwards compatability
      try {
        data.filterTree = JSON.parse(data.filterTree)
      } catch (e) {
        data.filterTree = CQLUtils.transformCQLToFilter(data.cql)
      }
    }
    return PartialAssociatedModel.prototype.set.call(this, data, ...args)
  },
  toJSON(...args) {
    const json = PartialAssociatedModel.prototype.toJSON.call(this, ...args)
    if (typeof json.filterTree === 'object') {
      json.filterTree = JSON.stringify(json.filterTree)
    }
    return json
  },
  //in the search we are checking for whether or not the model
  //only contains 5 items to know if we can search or not
  //as soon as the model contains more than 5 items, we assume
  //that we have enough values to search
  defaults() {
    return _merge(
      {
        cql: "anyText ILIKE ''",
        filterTree: { property: 'anyText', value: '', type: 'ILIKE' },
        associatedFormModel: undefined,
        excludeUnnecessaryAttributes: true,
        count: properties.resultCount,
        start: 1,
        federation: 'enterprise',
        sorts: [
          {
            attribute: 'modified',
            direction: 'descending',
          },
        ],
        result: undefined,
        serverPageIndex: 1,
        type: 'text',
        isLocal: false,
        isOutdated: false,
        'detail-level': undefined,
        spellcheck: false,
        phonetics: false,
      },
      user.getQuerySettings().toJSON()
    )
  },
  resetToDefaults(overridenDefaults) {
    const defaults = _.omit(this.defaults(), [
      'isLocal',
      'serverPageIndex',
      'result',
    ])
    this.set(_merge(defaults, overridenDefaults))
    this.trigger('resetToDefaults')
  },
  applyDefaults() {
    this.set(_.pick(this.defaults(), ['sorts', 'federation', 'sources']))
  },
  revert() {
    this.trigger('revert')
  },
  isLocal() {
    return this.get('isLocal')
  },
  initialize() {
    this.currentIndexForSource = {}
    this.state = [{}]

    _.bindAll.apply(_, [this].concat(_.functions(this))) // underscore bindAll does not take array arg
    this.set('id', this.getId())
    this.listenTo(this, 'change:cql', () => {
      this.set('isOutdated', true)
      this.resetCurrentIndexForSourceGroup()
    })
    this.listenTo(
      user.get('user').get('preferences'),
      'change:resultCount',
      () => {
        this.resetCurrentIndexForSourceGroup()
      }
    )

    const sync = () => {
      // needs to be updated to handle the lazyResults, right now this doesn't do anything
      // this.dispatch(updateResults(this.get('result').toJSON())) //do we need this?  It's expensive
      this.set('serverPageIndex', serverPageIndex(this.state))

      const totalHits = this.get('result')
        .get('status')
        .reduce((total, status) => {
          if (status.get('id') !== 'cache') {
            return total + status.get('hits')
          }
          return total
        }, 0)

      this.set('totalHits', totalHits)
    }

    this.listenTo(this, 'change:result', () => {
      if (this.has('result')) {
        this.listenTo(this.get('result'), 'reset:results', sync)
        this.listenTo(this.get('result'), 'change', sync)
      }
    })
  },
  getSelectedSources() {
    const federation = this.get('federation')
    switch (federation) {
      case 'local':
        return [Sources.localCatalog]
        break
      case 'enterprise':
        return _.pluck(Sources.toJSON(), 'id')
        break
      case 'selected':
        return this.get('sources')
        break
    }
  },
  buildSearchData() {
    const data = this.toJSON()
    switch (data.federation) {
      case 'local':
        data.sources = [Sources.localCatalog]
        break
      case 'enterprise':
        data.sources = _.pluck(Sources.toJSON(), 'id')
        break
      case 'selected':
        // already in correct format
        break
    }

    data.count = user
      .get('user')
      .get('preferences')
      .get('resultCount')

    data.sorts = this.get('sorts')

    return _.pick(
      data,
      'sources',
      'start',
      'count',
      'timeout',
      'cql',
      'sorts',
      'id',
      'spellcheck',
      'phonetics'
    )
  },
  isOutdated() {
    return this.get('isOutdated')
  },
  startTieredSearchIfOutdated(ids) {
    if (this.isOutdated()) {
      this.startTieredSearch(ids)
    }
  },
  startSearchIfOutdated() {
    if (this.isOutdated()) {
      this.startSearch()
    }
  },
  startSearchFromFirstPage(options) {
    // this.dispatch(clearPages())
    // this.set('serverPageIndex', serverPageIndex(this.state))
    this.resetCurrentIndexForSourceGroup()
    this.startSearch(options)
  },
  startTieredSearch(ids) {
    this.set('federation', 'local')
    this.startSearch(undefined, searches => {
      $.when(...searches).then(() => {
        const queryResponse = this.get('result')
        if (queryResponse && queryResponse.isUnmerged()) {
          this.listenToOnce(
            queryResponse,
            'change:merged',
            handleTieredSearchLocalFinish.bind(this, ids)
          )
        } else {
          handleTieredSearchLocalFinish.call(this, ids)
        }
      })
    })
  },
  async preQueryPlugin(data) {
    return data
  },
  startSearch(options, done) {
    this.set('isOutdated', false)
    if (this.get('cql') === '') {
      return
    }
    options = _.extend(
      {
        limitToDeleted: false,
        limitToHistoric: false,
      },
      options
    )
    this.cancelCurrentSearches()

    const data = Common.duplicate(this.buildSearchData())
    data.batchId = Common.generateUUID()
    if (options.resultCountOnly) {
      data.count = 0
    }

    // Data.sources is set in `buildSearchData` based on which sources you have selected.
    const sources = data.sources
    const initialStatus = sources.map(src => ({
      id: src,
    }))
    let result
    if (this.get('result') && this.get('result').get('results')) {
      result = this.get('result')
      result.emptyQueue()
      result.get('lazyResults')._updatePersistantSorts(this.get('sorts'))
      result.get('lazyResults').reset({
        sorts: this.get('sorts'),
        sources,
      })
      result.setColor(this.getColor())
      result.setQueryId(this.getId())
      result.set('selectedResultTemplate', this.get('detail-level'))
      result.set('merged', true)
      result.get('queuedResults').reset()
      result.get('results').reset(options.results || [])
      result
        .get('status')
        .reset(
          options.status ? options.status.concat(initialStatus) : initialStatus
        )
    } else {
      result = new QueryResponse({
        lazyResults: new LazyQueryResults({
          sorts: this.get('sorts'),
          sources,
        }),
        queryId: this.getId(),
        color: this.getColor(),
        status: initialStatus,
        selectedResultTemplate: this.get('detail-level'),
      })
      this.set({
        result,
      })
    }

    result.set('initiated', Date.now())
    result.set('resultCountOnly', options.resultCountOnly)
    // ResultSort.sortResults(this.get('sorts'), result.get('results')) // why sort before searching?

    if (!properties.isCacheDisabled) {
      sources.unshift('cache')
    }

    let cqlString = data.cql
    if (options.limitToDeleted) {
      cqlString = limitToDeleted(cqlString)
    } else if (options.limitToHistoric) {
      cqlString = limitToHistoric(cqlString)
    }

    const selectedSources = data.sources

    const harvestedSources = Sources.getHarvested()

    const isHarvested = id => harvestedSources.includes(id) && id !== 'cache'
    const isFederated = id => !harvestedSources.includes(id) && id !== 'cache'

    this.currentIndexForSourceGroup = this.nextIndexForSourceGroup
    const localSearchToRun = {
      ...data,
      cql: cqlString,
      srcs: selectedSources.filter(isHarvested),
      start: this.currentIndexForSourceGroup.local,
    }

    const federatedSearchesToRun = selectedSources
      .filter(isFederated)
      .map(source => ({
        ...data,
        cql: cqlString,
        srcs: [source],
        start: this.currentIndexForSourceGroup[source],
      }))

    const searchesToRun = [localSearchToRun, ...federatedSearchesToRun].filter(
      search => search.srcs.length > 0
    )

    if (!properties.isCacheDisabled) {
      searchesToRun.push({
        ...data,
        // since the "cache" source will return all cached results, need to
        // limit the cached results to only those from a selected source. This adds metacard_source to the cql string.
        cql: CacheSourceSelector.trimCacheSources(cqlString, sources),
        srcs: ['cache'],
        start: query.getStartIndexForSource('cache'),
      })
    }

    const currentSearches = this.preQueryPlugin(searchesToRun)

    currentSearches.then(currentSearches => {
      if (currentSearches.length === 0) {
        announcement.announce({
          title: 'Search "' + this.get('title') + '" cannot be run.',
          message: properties.i18n['search.sources.selected.none.message'],
          type: 'warn',
        })
        this.currentSearches = []
        return
      }

      this.currentSearches = currentSearches.map(search => {
        delete search.sources // This key isn't used on the backend and only serves to confuse those debugging this code.

        // `result` is QueryResponse
        return result.fetch({
          customErrorHandling: true,
          data: JSON.stringify(search),
          remove: false,
          dataType: 'json',
          contentType: 'application/json',
          method: 'POST',
          processData: false,
          timeout: properties.timeout,
          success(model, response, options) {
            response.options = options
            if (options.resort === true) {
              model.get('results').sort()
            }
          },
          error(model, response, options) {
            if (response.status === 401) {
              const providerUrl = response.responseJSON.url
              const sourceId = response.responseJSON.id

              const link = React.createElement(
                'a',
                {
                  href: providerUrl,
                  target: '_blank',
                  style: {
                    color: `${props =>
                      readableColor(props.theme.negativeColor)}`,
                  },
                },
                `Click Here To Authenticate ${sourceId}`
              )
              announcement.announce({
                title: `Source ${sourceId} is Not Authenticated`,
                message: link,
                type: 'error',
              })
            }

            // TODO: Need to do something here?
            const srcStatus = result.get('status').get(search.src)
            if (srcStatus) {
              srcStatus.set({
                successful: false,
                pending: false,
              })
            }
            response.options = options
          },
        })
      })
      if (typeof done === 'function') {
        done(this.currentSearches)
      }
    })
  },
  currentSearches: [],
  cancelCurrentSearches() {
    this.currentSearches.forEach(request => {
      request.abort('Canceled')
    })
    const result = this.get('result')
    if (result) {
      result.get('lazyResults').cancel()
      result.emptyQueue()
    }
    this.currentSearches = []
  },
  clearResults() {
    this.cancelCurrentSearches()
    this.set({
      result: undefined,
    })
  },
  setSources(sources) {
    const sourceArr = []
    sources.each(src => {
      if (src.get('available') === true) {
        sourceArr.push(src.get('id'))
      }
    })
    if (sourceArr.length > 0) {
      this.set('sources', sourceArr.join(','))
    } else {
      this.set('sources', '')
    }
  },
  getId() {
    if (this.get('id')) {
      return this.get('id')
    } else {
      const id = this._cloneOf || this.id || Common.generateUUID()
      this.set('id')
      return id
    }
  },
  setColor(color) {
    this.set('color', color)
  },
  getColor() {
    return this.get('color')
  },
  color() {
    return this.get('color')
  },
  hasPreviousServerPage() {
    return serverPageIndex(this.state) > 1
  },
  hasNextServerPage() {
    const pageSize = user
      .get('user')
      .get('preferences')
      .get('resultCount')

    const totalHits = this.get('totalHits')
    const currentPage = serverPageIndex(this.state)
    return currentPage < Math.ceil(totalHits / pageSize)
  },
  getPreviousServerPage() {
    // this.dispatch(previousPage())
    // this.set('serverPageIndex', serverPageIndex(this.state))
    this.setNextIndexForSourceGroupToPrevPage()
    this.startSearch()
  },
  /**
   * Much simpler than seeing if a next page exists
   */
  hasPreviousServerPage() {
    return this.pastIndexesForSourceGroup.length > 0
  },
  hasNextServerPage() {
    const currentStatus = this.get('result')
      ? this.get('result').get('lazyResults').status
      : {}
    const harvestedSources = Sources.getHarvested()
    const isLocal = id => {
      return harvestedSources.includes(id)
    }
    const maxIndexSeenLocal =
      Object.values(currentStatus)
        .filter(status => isLocal(status.id))
        .reduce((amt, status) => {
          amt = amt + status.count
          return amt
        }, 0) + this.currentIndexForSourceGroup.local
    const maxIndexPossibleLocal = Object.values(currentStatus)
      .filter(status => isLocal(status.id))
      .reduce((amt, status) => {
        amt = amt + status.hits
        return amt
      }, 0)
    if (maxIndexSeenLocal <= maxIndexPossibleLocal) {
      return true
    }

    return Object.values(currentStatus)
      .filter(status => !isLocal(status.id))
      .some(status => {
        const maxIndexPossible = status.hits
        const count = status.count
        const maxIndexSeen = count + this.currentIndexForSourceGroup[status.id]
        return maxIndexSeen <= maxIndexPossible
      })
  },
  getNextServerPage() {
    // this.dispatch(nextPage())
    // this.set('serverPageIndex', serverPageIndex(this.state))
    this.setNextIndexForSourceGroupToNextPage(this.getSelectedSources())
    this.startSearch()
  },
  // get the starting offset (beginning of the server page) for the given source
  getStartIndexForSource(src) {
    return currentIndexForSource(this.state)[src] || 1
  },
  resetCurrentIndexForSourceGroup() {
    this.currentIndexForSourceGroup = {}
    if (this.get('result')) {
      this.get('result')
        .get('lazyResults')
        ._resetSources([])
    }
    this.setNextIndexForSourceGroupToNextPage(this.getSelectedSources())
    this.pastIndexesForSourceGroup = []
  },
  currentIndexForSourceGroup: {},
  pastIndexesForSourceGroup: [],
  nextIndexForSourceGroup: {},
  /**
   * Update the next index to be the prev page
   */
  setNextIndexForSourceGroupToPrevPage() {
    if (this.pastIndexesForSourceGroup.length > 0) {
      this.nextIndexForSourceGroup = this.pastIndexesForSourceGroup.pop()
    } else {
      console.log('this should not happen')
    }
  },
  /**
   * Update the next index to be the next page
   */
  setNextIndexForSourceGroupToNextPage(sources) {
    this.pastIndexesForSourceGroup.push(this.nextIndexForSourceGroup)
    this.nextIndexForSourceGroup = this._calculateNextIndexForSourceGroupNextPage(
      sources
    )
  },
  /**
   * Get what the next index should be for going forward
   */
  _calculateNextIndexForSourceGroupNextPage(sources) {
    const harvestedSources = Sources.getHarvested()
    const isLocal = id => {
      return harvestedSources.includes(id)
    }
    const federatedSources = sources.filter(id => {
      return !isLocal(id)
    })
    const currentStatus = this.get('result')
      ? this.get('result').get('lazyResults').status
      : {}

    const maxLocalStart = Math.max(
      1,
      Object.values(currentStatus)
        .filter(status => isLocal(status.id))
        .filter(status => status.hits !== undefined)
        .reduce((blob, status) => {
          return blob + status.hits
        }, 0)
    )
    return Object.values(currentStatus).reduce(
      (blob, status) => {
        if (isLocal(status.id)) {
          blob['local'] = Math.min(maxLocalStart, blob['local'] + status.count)
        } else {
          blob[status.id] = Math.min(
            status.hits !== undefined ? status.hits : 1,
            blob[status.id] + status.count
          )
        }
        return blob
      },
      {
        local: 1,
        ...federatedSources.reduce((blob, id) => {
          blob[id] = 1
          return blob
        }, {}),
        ...this.currentIndexForSourceGroup,
      }
    )
  },
  getResultsRangeLabel(resultsCollection) {
    const results = resultsCollection.length
    const hits = _.filter(
      this.get('result')
        .get('status')
        .toJSON(),
      status => status.id !== 'cache'
    ).reduce((hits, status) => (status.hits ? hits + status.hits : hits), 0)

    if (results === 0) {
      return '0 results'
    } else if (results > hits) {
      return results + ' results'
    }

    const serverPageSize = user.get('user>preferences>resultCount')
    const startingIndex = serverPageIndex(this.state) * serverPageSize
    const endingIndex = startingIndex + resultsCollection.length

    return startingIndex + 1 + '-' + endingIndex + ' of ' + hits
  },
})
module.exports = plugin(Query)
