import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '../../api/ordersApi'

export function useCreateOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ordersApi.create,
    onSuccess: (order) => {
      void queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.setQueryData(['orders', order.id], order)
    },
  })
}

export function useCancelOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ordersApi.cancel,
    onSuccess: (order) => {
      queryClient.setQueryData(['orders', order.id], order)
      void queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}
